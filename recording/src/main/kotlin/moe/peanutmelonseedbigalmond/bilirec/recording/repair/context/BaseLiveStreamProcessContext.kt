package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import moe.peanutmelonseedbigalmond.bilirec.coroutine.withReentrantLock
import moe.peanutmelonseedbigalmond.bilirec.flv.reader.FlvTagReader
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingRuleChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagGroupProcessChain
import org.greenrobot.eventbus.EventBus
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

abstract class BaseLiveStreamProcessContext(
    protected val inputStream: InputStream,
    protected val room: Room,
    protected val outputFileNamePrefix: String,
    protected val coroutineContext: CoroutineContext
) : SuspendableCloseable {
    protected abstract val logger: BaseLogging
    protected var flvTagReader: FlvTagReader? = null
    protected var flvTagWriter: BaseFlvTagWriter? = null
    protected val scope = CoroutineScope(coroutineContext + SupervisorJob())
    protected lateinit var processChain: FlvTagGroupProcessChain
    protected lateinit var tagGroupRule: TagGroupingRuleChain

    @Volatile
    protected var closed = false
    private val writeLock = Mutex()
    private var flvTagReadJob: Job? = null
    open suspend fun start() = withContext(scope.coroutineContext) {
        flvTagWriter = createFlvTagWriter()
        flvTagReader = createFlvTagReader()
        tagGroupRule = createTagGroupingRule()
        processChain = createTagProcessChain()
        scope.launch {
            flvTagReadJob = createFlvTagReadJob()
            if (!flvTagReadJob!!.isActive) {
                flvTagReadJob!!.start()
            }
        }
    }

    protected abstract fun createFlvTagWriter(): BaseFlvTagWriter
    protected abstract fun createFlvTagReader(): FlvTagReader
    protected abstract fun createTagProcessChain(): FlvTagGroupProcessChain
    protected abstract fun createTagGroupingRule(): TagGroupingRuleChain
    protected abstract fun onTagGroupRead(tagGroup: TagGroup)

    protected open fun createFlvTagReadJob(): Job {
        return scope.launch {
            logger.info("开始接收直播流")
            try {
                if (flvTagReader == null) return@launch
                if (flvTagWriter == null) return@launch

                while (isActive) {
                    try {
                        writeLock.lock()
                        processChain.readTagGroupList().forEach {
                            onTagGroupRead(it)
                        }
                    } finally {
                        withContext(NonCancellable) {
                            writeLock.unlock()
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EventBus.getDefault().post(RecordingThreadErrorEvent(room, e))
            } finally {
                withContext(NonCancellable) {
                    flvTagReader?.close()
                    flvTagReader = null
                    flvTagWriter?.close()
                    flvTagWriter = null

                    EventBus.getDefault().post(RecordingThreadExitedEvent(room))

                    logger.info("录制结束")
                }
            }
        }
    }

    protected fun flvReadDataSource() = sequence {
        val next = runBlocking { flvTagReader!!.readNextTagAsync() }
        yield(next)
    }

    override suspend fun close() {
        if (closed) return
        closed = true
        writeLock.withReentrantLock {
            this.flvTagReadJob?.cancelAndJoin()
            this.flvTagReadJob = null
            scope.cancel()
        }
    }
}