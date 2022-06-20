package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
import java.io.File
import java.time.OffsetDateTime
import java.util.concurrent.locks.ReentrantLock

class RoomRecordingTaskController(
    private val room: Room,
) : Closeable {
    private val logger = LoggingFactory.getLogger(this.room.roomConfig.roomId, this::class.java)
    private val startAndStopLock = ReentrantLock()

    @Volatile
    private var videoRecordingTask: BaseRecordTask? = null
    private var danmakuRecordingTask: BaseRecordTask? = null

    @Volatile
    var started = false
        private set

    @Volatile
    private var closed = false

    suspend fun prepareAsync() {
        EventBus.getDefault().register(this)
        withContext(Dispatchers.IO) {
            if (danmakuRecordingTask != null) {
                danmakuRecordingTask!!.closeQuietly()
                danmakuRecordingTask = null
            }
            danmakuRecordingTask = DanmakuRecordTask(this@RoomRecordingTaskController.room)
            danmakuRecordingTask!!.prepare()
            if (this@RoomRecordingTaskController.room.roomConfig.enableAutoRecord) {
                videoRecordingTask = RecordTaskFactory.getRecordTask(this@RoomRecordingTaskController.room)
                videoRecordingTask!!.prepare()
            }
        }
    }

    override fun close() {
        runBlocking {
            startAndStopLock.lock()
            if (closed) {
                startAndStopLock.unlock()
                return@runBlocking
            }

            requestStopAsync()
            withContext(Dispatchers.IO) {
                videoRecordingTask?.closeQuietly()
                danmakuRecordingTask!!.closeQuietly()
            }
            videoRecordingTask = null
            danmakuRecordingTask = null
        }
        EventBus.getDefault().unregister(this@RoomRecordingTaskController)
        startAndStopLock.unlock()
    }

    suspend fun requestStopAsync() {
        withContext(Dispatchers.IO) {
            startAndStopLock.lock()
            if (!started) {
                startAndStopLock.unlock()
                return@withContext
            }
            try {
                videoRecordingTask?.stopRecording()
                danmakuRecordingTask!!.stopRecording()
                started = false
            } finally {
                startAndStopLock.unlock()
            }
        }
    }

    suspend fun requestStartAsync() {
        withContext(Dispatchers.IO) {
            startAndStopLock.lock()
            if (started) {
                startAndStopLock.unlock()
                return@withContext
            }
            try {
                val startTime = OffsetDateTime.now()
                val baseDir = File(
                    removeIllegalChar(
                        "${this@RoomRecordingTaskController.room.roomConfig.roomId}-${this@RoomRecordingTaskController.room.userName}"
                    )
                )
                if (!baseDir.exists()) baseDir.mkdirs()
                val baseFile =
                    File(baseDir, removeIllegalChar(generateFileName(this@RoomRecordingTaskController.room, startTime)))
                videoRecordingTask!!.startAsync(baseFile.canonicalPath)
                started = true
            } finally {
                startAndStopLock.unlock()
            }
        }
    }

    // 等待录制的视频文件创建了之后再启动弹幕录制
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

    private fun removeIllegalChar(str: String): String {
        return str.replace("[\\\\/:*?\"<>|]".toRegex(), " ")
    }
}