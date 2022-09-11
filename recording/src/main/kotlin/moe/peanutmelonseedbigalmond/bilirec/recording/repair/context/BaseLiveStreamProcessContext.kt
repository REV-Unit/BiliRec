package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import kotlinx.coroutines.*
import moe.peanutmelonseedbigalmond.bilirec.flv.reader.FlvTagReader
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingRuleChain
import org.greenrobot.eventbus.EventBus
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

abstract class BaseLiveStreamProcessContext(
    protected val inputStream: InputStream,
    protected val room: Room,
    protected val outputFileNamePrefix: String,
    protected val coroutineCtx: CoroutineContext
) : SuspendableCloseable, CoroutineScope by CoroutineScope(coroutineCtx) {
    protected val logger = LoggingFactory.getLogger(this.room.roomId, this)
    protected var recordLoop: Job? = null
    protected var tagReader: FlvTagReader? = null
    protected var tagWriter: BaseFlvTagWriter? = null
    protected abstract val flvTagGroupingRuleBuilder: TagGroupingRuleChain.Builder
    private lateinit var flvTagGroupingRule: TagGroupingRuleChain
    private var closed = false

    abstract fun createFlvTagReader(): FlvTagReader
    abstract fun createFlvTagWriter(): BaseFlvTagWriter

    fun start() {
        tagReader = createFlvTagReader()
        tagWriter = createFlvTagWriter()
        flvTagGroupingRule = flvTagGroupingRuleBuilder.build()

        recordLoop = createRecordLoop()
    }

    protected fun createRecordLoop(): Job = launch(Dispatchers.IO) {
        try {
            logger.info("开始接收直播流")
            while (isActive && tagReader != null && tagWriter != null) {
                val tag = tagReader!!.readNextTagAsync() ?: break
                flvTagGroupingRule.proceed(tag)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EventBus.getDefault().post(RecordingThreadErrorEvent(room, e))
        } finally {
            tagReader?.close()
            tagReader = null
            tagWriter?.close()
            tagWriter = null

            EventBus.getDefault().post(RecordingThreadExitedEvent(room))
            logger.info("录制结束")
        }
    }


    protected open fun onTagGroupReceived(tagGroup: TagGroup) {
        tagWriter?.writeTagGroup(tagGroup)
    }

    override suspend fun close() {
        if (closed) return
        closed = true
        if (recordLoop?.isActive == true) {
            recordLoop?.cancelAndJoin()
        }
        recordLoop = null
        this.coroutineContext.cancel()
    }
}