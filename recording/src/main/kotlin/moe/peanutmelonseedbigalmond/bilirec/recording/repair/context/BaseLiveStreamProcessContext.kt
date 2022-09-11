package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import moe.peanutmelonseedbigalmond.bilirec.coroutine.withReentrantLock
import moe.peanutmelonseedbigalmond.bilirec.flv.reader.FlvTagReader
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingProcessChain
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain
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
    private lateinit var processChain: FlvTagProcessChain<List<Tag>>
    private lateinit var tagGroupChain: TagGroupingProcessChain

    @Volatile
    protected var closed = false
    private val writeLock = Mutex()
    private var flvTagReadJob: Job? = null
    open suspend fun start() = withContext(scope.coroutineContext) {
        flvTagWriter = createFlvTagWriter()
        flvTagReader = createFlvTagReader()
        processChain = createTagProcessChainWithoutAction().collect(::onTagGroupRead)
        tagGroupChain = createTagGroupingProcessChainWithoutAction().collect { processChain.startProceed(it) }
        scope.launch {
            flvTagReadJob = createFlvTagReadJob()
            if (!flvTagReadJob!!.isActive) {
                flvTagReadJob!!.start()
            }
        }
    }

    protected abstract fun createFlvTagWriter(): BaseFlvTagWriter
    protected abstract fun createFlvTagReader(): FlvTagReader
    protected abstract fun createTagProcessChainWithoutAction(): FlvTagProcessChain<List<Tag>>
    protected abstract fun createTagGroupingProcessChainWithoutAction(): TagGroupingProcessChain
    protected abstract fun onTagGroupRead(tagGroup: List<Tag>)

    protected open fun createFlvTagReadJob(): Job {
        return scope.launch {
            logger.info("开始接收直播流")
            try {
                if (flvTagReader == null) return@launch
                if (flvTagWriter == null) return@launch

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