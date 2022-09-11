package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagwriter

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.isKeyframeData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.*
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.FlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class RepairFlvTagWriter(private val roomId: Long, private val flvTagWriter: FlvTagWriter) : BaseFlvTagWriter() {
    private val logger = LoggingFactory.getLogger(roomId, this)
    private var previousTag: Tag? = null
    private var scriptTag: Tag? = null
    private val previousScriptTagBinaryLength = AtomicLong(0)

    override fun writeTagGroup(tagGroup: List<Tag>) {
        tagGroup.forEach(this::writeTag)
    }

    private fun writeTag(tag: Tag) {
        when (tag.getTagType()) {
            TagType.SCRIPT -> {
                if (scriptTag == null) {
                    flvTagWriter.writeFlvData(tag)
                    scriptTag = tag
                    previousScriptTagBinaryLength.set(tag.binaryLength)
                } else {
                    logger.warn("写入直播数据中途遇到意外的Script数据块，忽略")
                    return
                }
            }
            TagType.VIDEO, TagType.AUDIO -> {
                if (scriptTag == null) {
                    logger.warn("未收到 ScriptTag 数据")
                    writeTag(newScriptTag())
                }
                writeFlvData(tag)
            }
            else -> {}
        }
        previousTag = tag
    }

    // 除了Script Data以外
    private fun writeFlvData(tag: Tag) {
        if (tag.isKeyframeData()) {
            // 重新计算视频长度
            val scriptDataArray = (scriptTag?.data as ScriptData)[1] as ScriptDataEcmaArray
            with(scriptDataArray) {
                val oldDuration = (this["duration"] as ScriptDataNumber?)?.value ?: 0.0
                val newValue = oldDuration.coerceAtLeast(tag.getTimeStamp() / 1000.0)
                this["duration"] = ScriptDataNumber.assign(newValue)
            }

            // 更新视频关键帧
            (scriptDataArray["keyframes"] as KeyframesObject).addKeyframe(
                tag.getTimeStamp().toDouble(),
                flvTagWriter.getFileLength()
            )

            overwriteFlvScriptData(scriptTag!!)
        }

        flvTagWriter.writeFlvData(tag)
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

    private fun overwriteFlvScriptData(data: Tag) {
        val bos = ByteArrayOutputStream()
        data.writeTo(bos)
        if (bos.size().toLong() != previousScriptTagBinaryLength.get()) {
            logger.warn("Script Tag 数据块长度不匹配，忽略（old=${previousScriptTagBinaryLength}, new=${bos.size().toLong()}）")
            logger.warn("previousScriptTag=$scriptTag, current=$data")
            return
        }
        flvTagWriter.writeFlvScriptTag(data)
        this.previousScriptTagBinaryLength.set(data.binaryLength)
        bos.close()
    }

    override fun close() {
        flvTagWriter.close()
    }
}