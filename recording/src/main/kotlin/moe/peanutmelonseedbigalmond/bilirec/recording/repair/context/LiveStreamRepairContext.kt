package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import kotlinx.coroutines.sync.Mutex
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.FrameType
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.reader.FlvTagReader
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.VideoData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.*
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.FlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingRuleChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.EndTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.GOPGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.HeaderTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.ScriptTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagGroupProcessChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.ScriptTagNormalizeGroupProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.TagTimestampOffsetGroupProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.UpdateTagTimestampGroupProcessNode
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

class LiveStreamRepairContext(
    inputStream: InputStream,
    room: Room,
    outputFileNamePrefix: String,
    coroutineContext: CoroutineContext
) : BaseLiveStreamProcessContext(inputStream, room, outputFileNamePrefix, coroutineContext) {
    override val logger: BaseLogging
        get() = LoggingFactory.getLogger(room.roomId, this)
    private val writeLock = Mutex()

    @Volatile
    private var previousScriptTagBinaryLength = AtomicLong(0)

    @Volatile
    private lateinit var previousScriptTag: Tag

    override fun createFlvTagWriter(): BaseFlvTagWriter {
        return FlvTagWriter("$outputFileNamePrefix.flv")
    }

    override fun createFlvTagReader(): FlvTagReader {
        return FlvTagReader(inputStream, this.logger)
    }

    override fun createTagProcessChain(): FlvTagGroupProcessChain {
        return FlvTagGroupProcessChain.Builder()
            .addNode(ScriptTagNormalizeGroupProcessNode())
            .addNode(TagTimestampOffsetGroupProcessNode())
            .addNode(UpdateTagTimestampGroupProcessNode())
            .setDataSource(tagGroupRule.proceed())
            .setLogger(logger)
            .build()
    }

    override fun createTagGroupingRule(): TagGroupingRuleChain {
        return TagGroupingRuleChain.Builder()
            .addRule(ScriptTagGroupingRule())
            .addRule(EndTagGroupingRule())
            .addRule(HeaderTagGroupingRule())
            .addRule(GOPGroupingRule())
            .setLogger(logger)
            .setDataSource(flvReadDataSource())
            .build()
    }

    override fun onTagGroupRead(tagGroup: List<Tag>) {
        tagGroup.forEach(::writeTag)
    }

    private fun writeTag(tag: Tag?) {
        if (tag == null) return
        synchronized(writeLock) {
            if (closed) return
            when (tag.getTagType()) {
                TagType.SCRIPT -> {
                    flvTagWriter?.writeFlvHeader()
                    previousScriptTag = tag
                    flvTagWriter?.writeFlvScriptTag(tag)
                    previousScriptTagBinaryLength.set(tag.binaryLength)
                }
                TagType.VIDEO -> {
                    if (::previousScriptTag.isInitialized) {
                        writeVideoChunk(tag)
                    } else {
                        writeTag(newScriptTag())
                    }
                }
                TagType.AUDIO -> {
                    if (::previousScriptTag.isInitialized) {
                        flvTagWriter?.writeFlvData(tag)
                    } else {
                        writeTag(newScriptTag())
                    }
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }

    private fun newScriptTag(): Tag {
        logger.debug("构造新的ScriptTag")
        val tag = Tag()
        val list = LinkedList<BaseScriptDataValue>()
        list.add(ScriptDataString().also { it.value = "onMetaData" })
        val arrayData = ScriptDataEcmaArray()
        arrayData["keyframes"] = KeyframesObject()
        list.add(arrayData)
        val tagData = ScriptData(list)
        tag.setTagType(TagType.SCRIPT)
        tag.data = tagData
        tag.setStreamId(0)
        tag.setDataSize(tagData.binaryLength.toInt())
        tag.setTimeStamp(0)
        return tag
    }

    private fun writeVideoChunk(tag: Tag) {
        if (tag.data !is VideoData) {
            logger.warn("data 不是视频数据块，忽略")
            return
        }

        val scriptDataArray = (this.previousScriptTag.data as ScriptData)[1] as ScriptDataEcmaArray

        // 更新ScriptData中的视频长度
        with(scriptDataArray) {
            val oldDuration = (this["duration"] as ScriptDataNumber?)?.value ?: 0.0
            val newValue = oldDuration.coerceAtLeast(tag.getTimeStamp() / 1000.0) // Script tag 中的长度是以秒为单位的
            this["duration"] = ScriptDataNumber.assign(newValue)
        }

        // 更新视频关键帧
        if ((tag.data as VideoData).frameType == FrameType.KEY_FRAME) {
            (scriptDataArray["keyframes"] as KeyframesObject).addKeyframe(
                tag.getTimeStamp().toDouble(),
                flvTagWriter?.getFileLength()!!
            )
        }
        flvTagWriter!!.writeFlvData(tag)
        overwriteFlvScriptData(this.previousScriptTag)
//        this.flvWriter!!.flush()
    }

    private fun overwriteFlvScriptData(data: Tag) {
        val bos = ByteArrayOutputStream()
        data.writeTo(bos)
        if (bos.size().toLong() != previousScriptTagBinaryLength.get()) {
            logger.warn("Script Tag 数据块长度不匹配，忽略（old=${previousScriptTagBinaryLength}, new=${bos.size().toLong()}）")
            logger.warn("previousScriptTag=$previousScriptTag, current=$data")
            return
        }
        flvTagWriter!!.writeFlvScriptTag(data)
        this.previousScriptTagBinaryLength.set(data.binaryLength)
        bos.close()
    }
}
