package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import moe.peanutmelonseedbigalmond.bilirec.coroutine.withReentrantLock
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.FrameType
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.reader.FlvTagReader
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.VideoData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.*
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.FlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingProcessChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.EndTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.GOPGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.HeaderTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl.ScriptTagGroupingRule
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.ScriptTagNormalizeProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.TagTimestampOffsetProcessNode
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.UpdateTagTimestampProcessNode
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

class LiveStreamRepairContext(
    private val inputStream: InputStream,
    private val room: Room,
    private val outputFileNamePrefix: String,
    coroutineContext: CoroutineContext
) : SuspendableCloseable, CoroutineScope by CoroutineScope(coroutineContext) {
    private val logger = LoggingFactory.getLogger(room.roomConfig.roomId, this)
    private val writeLock = Mutex()
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    @Volatile
    private var flvTagReader: FlvTagReader? = null

    @Volatile
    private var previousScriptTagBinaryLength = AtomicLong(0)

    @Volatile
    private lateinit var previousScriptTag: Tag

    @Volatile
    private var flvWriter: FlvTagWriter? = null
    private lateinit var processChain: FlvTagProcessChain<List<Tag>>
    private lateinit var tagGroupChain: TagGroupingProcessChain
    private var flvWriteJob: Job? = null

    @Volatile
    private var closed = false

    suspend fun start() = withContext(scope.coroutineContext) {
        flvWriter = FlvTagWriter("$outputFileNamePrefix.flv")
        this@LiveStreamRepairContext.flvTagReader = FlvTagReader(inputStream, this@LiveStreamRepairContext.logger)
        processChain = FlvTagProcessChain<List<Tag>>(this@LiveStreamRepairContext.logger)
            .addProcessNode(ScriptTagNormalizeProcessNode())
            .addProcessNode(TagTimestampOffsetProcessNode())
            .addProcessNode(UpdateTagTimestampProcessNode())
            .collect(this@LiveStreamRepairContext::writeTagGroup)
        tagGroupChain = TagGroupingProcessChain(this@LiveStreamRepairContext.logger)
            .addRule(ScriptTagGroupingRule())
            .addRule(EndTagGroupingRule())
            .addRule(HeaderTagGroupingRule())
            .addRule(GOPGroupingRule())
            .collect { processChain.startProceed(it) }
        launch { this@LiveStreamRepairContext.flvWriteJob = createFlvWriteJob() }
    }

    override suspend fun close() {
        if (closed) return
        closed = true
        writeLock.withReentrantLock {
            this.flvWriteJob?.cancelAndJoin()
            this.flvWriteJob = null
            scope.cancel()
        }
    }

    private fun createFlvWriteJob(): Job {
        return scope.launch {
            logger.info("开始接收直播流")
            try {
                if (this@LiveStreamRepairContext.flvTagReader == null) return@launch
                if (this@LiveStreamRepairContext.flvWriter == null) return@launch

                while (isActive) {
                    try {
                        writeLock.lock()
                        val tag = flvTagReader?.readNextTagAsync() ?: break
                        tagGroupChain.proceed(tag)
                    } finally {
                        withContext(NonCancellable) {
                            writeLock.unlock()
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EventBus.getDefault().post(RecordingThreadErrorEvent(this@LiveStreamRepairContext.room, e))
            } finally {
                withContext(NonCancellable) {
                    this@LiveStreamRepairContext.flvTagReader?.close()
                    this@LiveStreamRepairContext.flvTagReader = null
                    this@LiveStreamRepairContext.flvWriter?.close()
                    this@LiveStreamRepairContext.flvWriter = null

                    EventBus.getDefault().post(RecordingThreadExitedEvent(this@LiveStreamRepairContext.room))

                    logger.info("录制结束")
                }
            }
        }
    }

    private fun writeTagGroup(tags: List<Tag>) {
        tags.forEach(::writeTag)
    }

    private fun writeTag(tag: Tag?) {
        if (tag == null) return
        synchronized(writeLock) {
            if (closed) return
            when (tag.getTagType()) {
                TagType.SCRIPT -> {
                    this@LiveStreamRepairContext.flvWriter?.writeFlvHeader()
                    this@LiveStreamRepairContext.previousScriptTag = tag
                    this@LiveStreamRepairContext.flvWriter?.writeFlvScriptTag(tag)
                    this@LiveStreamRepairContext.previousScriptTagBinaryLength.set(tag.binaryLength)
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
                        this@LiveStreamRepairContext.flvWriter?.writeFlvData(tag)
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
                this.flvWriter?.getFileLength()!!
            )
        }
        this.flvWriter!!.writeFlvData(tag)
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
        this.flvWriter!!.writeFlvScriptTag(data)
        this.previousScriptTagBinaryLength.set(data.binaryLength)
        bos.close()
    }
}

/**
 * TODO: 需要修改录制逻辑
 * 读取到流结尾 -> 判断主播是否停止直播 -> 是则视为这次直播结束，否则重新连接
 */