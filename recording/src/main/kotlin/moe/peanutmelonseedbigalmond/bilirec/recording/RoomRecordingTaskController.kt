package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.RecordTaskFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.DanmakuRecordTask
import okhttp3.internal.closeQuietly
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Closeable

class RoomRecordingTaskController(
    private val room: Room,
) : Closeable {
    private val logger = LoggingFactory.getLogger(this.room.roomConfig.roomId, this::class.java)

    @Volatile
    private var videoRecordingTask: BaseRecordTask? = null
    private var danmakuRecordingTask: BaseRecordTask? = null
    private var started = false

    suspend fun prepareAsync() {
        withContext(Dispatchers.IO) {
            EventBus.getDefault().register(this@RoomRecordingTaskController)
            if (danmakuRecordingTask != null) {
                danmakuRecordingTask!!.closeQuietly()
                danmakuRecordingTask = null
            }
            danmakuRecordingTask = DanmakuRecordTask(this@RoomRecordingTaskController.room)
            danmakuRecordingTask!!.prepare()
            if (this@RoomRecordingTaskController.room.roomConfig.enableAutoRecord){
                videoRecordingTask = RecordTaskFactory.getRecordTask(this@RoomRecordingTaskController.room)
                videoRecordingTask!!.prepare()
            }
        }
    }

    override fun close() {
        EventBus.getDefault().unregister(this@RoomRecordingTaskController)
    }

    suspend fun requestStopAsync() {
        withContext(Dispatchers.IO) {
            videoRecordingTask?.stopRecording()
            danmakuRecordingTask!!.stopRecording()
            started = false
        }
    }

    suspend fun requestStartAsync(baseFileName: String) {
        withContext(Dispatchers.IO) {
            if (started) return@withContext
            videoRecordingTask!!.startAsync(baseFileName)
            started = true
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileOpened(e: RecordFileOpenedEvent) {
        if (e.roomId == this.room.roomConfig.roomId) {
            logger.info("新建录制文件：${e.baseFileName}")
            danmakuRecordingTask!!.startAsync(e.baseFileName)
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileClosed(e: RecordFileClosedEvent) {
        if (e.roomId == this.room.roomConfig.roomId) {
            logger.info("录制结束")
            danmakuRecordingTask!!.stopRecording()
        }
    }
}