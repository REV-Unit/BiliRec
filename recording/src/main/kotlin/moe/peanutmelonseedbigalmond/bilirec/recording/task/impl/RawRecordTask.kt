package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import moe.peanutmelonseedbigalmond.bilirec.coroutine.withReentrantLock
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseVideoRecordTask
import okhttp3.internal.closeQuietly
import org.greenrobot.eventbus.EventBus
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 *
 * 录制原始数据，不修复
 */
class RawRecordTask(
    room: Room,
    coroutineContext: CoroutineContext
) : BaseVideoRecordTask(room) {
    private val startAndStopLock = Mutex()
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean
        get() = mClosed

    @Volatile
    private var recordingJob: Job? = null
    override suspend fun close() {
        if (closed) return
        mClosed = true
        stopRecording()
        scope.cancel()
    }

    override suspend fun prepare() {
    }

    override suspend fun start(baseFileName: String) = startAndStopLock.withReentrantLock {
        withContext(scope.coroutineContext) {
            if (started) return@withContext
            createLiveStreamRepairContext()
            if (this@RawRecordTask.recordingJob == null) {
                this@RawRecordTask.recordingJob = createRecordingJob(baseFileName)
            }
            started = true
        }
    }

    override suspend fun stopRecording() = startAndStopLock.withReentrantLock {
        withContext(scope.coroutineContext) {
            if (!started) return@withContext
            started = false
            recordingJob?.cancelAndJoin()
            recordingJob = null
            logger.info("停止接收直播流")
            EventBus.getDefault().post(RecordFileClosedEvent(this@RawRecordTask.room.roomConfig.roomId))
        }
    }

    private fun createRecordingJob(baseFileName: String): Job {
        return scope.launch {
            val newFileName = File("${baseFileName}_raw.flv")
            val directory = newFileName.parentFile
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileOutputStream = newFileName.outputStream()
            EventBus.getDefault().post(RecordFileOpenedEvent(roomId = room.roomConfig.roomId, baseFileName))
            var len: Int
            val buffer = ByteArray(4096)
            try {
                while (isActive) {
                    len = withContext(Dispatchers.IO) { liveStream.read(buffer) }
                    if (len == -1) break

                    withContext(Dispatchers.IO) { fileOutputStream.write(buffer, 0, len) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EventBus.getDefault().post(RecordingThreadErrorEvent(room, e))
            } finally {
                withContext(NonCancellable) {
                    fileOutputStream.closeQuietly()
                    EventBus.getDefault().post(RecordingThreadExitedEvent(room))
                }
            }
        }
    }
}