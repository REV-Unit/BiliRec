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

/**
 *
 * 录制原始数据，不修复
 */
class RawRecordTask(
    room: Room,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BaseRecordTask(room), CoroutineScope by CoroutineScope(coroutineContext) {
    private val startAndStopLock = Mutex()

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean
        get() = mClosed

    @Volatile
    private var recordingJob: Job? = null
    override fun close() {
        runBlocking {
            startAndStopLock.withLock {
                if (closed)return@withLock
                mClosed = true
                stopRecording()
                this@RawRecordTask.cancel()
            }
        }
    }

    override fun prepare() {

    }

    override fun startAsync(baseFileName: String) {
        runBlocking(coroutineContext) {
            startAndStopLock.withLock{
                if (started) return@withLock
                createLiveStreamRepairContextAsync()
                if (this@RawRecordTask.recordingJob == null) {
                    this@RawRecordTask.recordingJob = createRecordingJob(baseFileName)
                }

                this@RawRecordTask.recordingJob!!.start()
                started = true
            }
        }
    }

    override fun stopRecording() {
        runBlocking(coroutineContext) {
            startAndStopLock.withLock {
                if (!started) return@withLock
                started = false
                logger.info("停止接收直播流")
                EventBus.getDefault().post(RecordFileClosedEvent(this@RawRecordTask.room.roomConfig.roomId))
            }
        }
    }

    private fun createRecordingJob(baseFileName: String) = launch(
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