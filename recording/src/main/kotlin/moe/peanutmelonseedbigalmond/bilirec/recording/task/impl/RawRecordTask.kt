package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import okhttp3.internal.closeQuietly
import org.greenrobot.eventbus.EventBus
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 *
 * 录制原始数据，不修复
 */
class RawRecordTask(
    room: Room,
) : BaseRecordTask(room) {
    private val startAndStopLock = Mutex()

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean
        get() = mClosed

    @Volatile
    private var recordingJob: Job? = null
    override suspend fun closeAsync() {
        startAndStopLock.withLock {
            if (closed) return@withLock
            mClosed = true
            stopRecordingAsync()
        }
    }

    override suspend fun prepareAsync() {
    }

    override suspend fun startAsync(baseFileName: String) {
        startAndStopLock.withLock {
            if (started) return@withLock
            createLiveStreamRepairContextAsync()
            if (this@RawRecordTask.recordingJob == null) {
                this@RawRecordTask.recordingJob = createRecordingJob(baseFileName)
            }

            this@RawRecordTask.recordingJob!!.start()
            started = true
        }
    }

    override suspend fun stopRecordingAsync() {
        startAndStopLock.withLock {
            if (!started) return@withLock
            started = false
            logger.info("停止接收直播流")
            EventBus.getDefault().post(RecordFileClosedEvent(this@RawRecordTask.room.roomConfig.roomId))
        }
    }

    private fun createRecordingJob(baseFileName: String): Job {
        return runBlocking {
            return@runBlocking launch(
                Dispatchers.IO,
                start = CoroutineStart.LAZY
            ) {
                val newFileName = File("${baseFileName}_raw.flv")
                val directory = newFileName.parentFile
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val fileOutputStream = newFileName.outputStream()
                EventBus.getDefault().post(RecordFileOpenedEvent(roomId = room.roomConfig.roomId, baseFileName))
                var len: Int
                val buffer = ByteArray(4096)
                while (isActive) {
                    try {
                        len = withContext(Dispatchers.IO) { liveStream.read(buffer) }
                        if (len == -1) break

                        withContext(Dispatchers.IO) { fileOutputStream.write(buffer, 0, len) }
                    } catch (_: CancellationException) {

                    } catch (e: Exception) {
                        EventBus.getDefault().post(RecordingThreadErrorEvent(room, e))
                    }
                }
                fileOutputStream.closeQuietly()
                withContext(NonCancellable) {
                    EventBus.getDefault().post(RecordingThreadExitedEvent(room))
                }
            }
        }
    }
}