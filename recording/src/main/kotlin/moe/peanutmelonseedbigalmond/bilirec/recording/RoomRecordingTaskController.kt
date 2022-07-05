package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.peanutmelonseedbigalmond.bilirec.closeQuietlyAsync
import moe.peanutmelonseedbigalmond.bilirec.interfaces.AsyncCloseable
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
import java.io.File
import java.time.OffsetDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class RoomRecordingTaskController(
    private val room: Room,
    coroutineContext: CoroutineContext
) : AsyncCloseable, CoroutineScope by CoroutineScope(coroutineContext) {
    private val logger = LoggingFactory.getLogger(this.room.roomConfig.roomId, this)
    private val startAndStopLock = Mutex()

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
        if (danmakuRecordingTask != null) {
            danmakuRecordingTask!!.closeQuietlyAsync()
            danmakuRecordingTask = null
        }
        danmakuRecordingTask = DanmakuRecordTask(this@RoomRecordingTaskController.room, coroutineContext)
        danmakuRecordingTask!!.prepareAsync()
        if (this@RoomRecordingTaskController.room.roomConfig.enableAutoRecord) {
            videoRecordingTask = RecordTaskFactory.getRecordTask(this@RoomRecordingTaskController.room)
            videoRecordingTask!!.prepareAsync()
        }
    }

    override suspend fun closeAsync() {
        startAndStopLock.withLock {
            if (closed) return@withLock

            requestStopAsync()
            videoRecordingTask?.closeQuietlyAsync()
            danmakuRecordingTask!!.closeQuietlyAsync()
            videoRecordingTask = null
            danmakuRecordingTask = null
        }
        EventBus.getDefault().unregister(this@RoomRecordingTaskController)
    }

    suspend fun requestStopAsync() {
        startAndStopLock.withLock {
            if (!started) return@withLock
            videoRecordingTask?.stopRecordingAsync()
            danmakuRecordingTask!!.stopRecordingAsync()
            started = false
        }
    }

    suspend fun requestStartAsync() {
        startAndStopLock.withLock {
            if (started) return@withLock
            if (!this@RoomRecordingTaskController.room.roomConfig.enableAutoRecord) return@withLock
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
        }
    }

    // 等待录制的视频文件创建了之后再启动弹幕录制
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileOpened(e: RecordFileOpenedEvent) {
        if (e.roomId == this.room.roomConfig.roomId) {
            logger.info("新建录制文件：${e.baseFileName}")
            launch { danmakuRecordingTask!!.startAsync(e.baseFileName) }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileClosed(e: RecordFileClosedEvent) {
        if (e.roomId == this.room.roomConfig.roomId) {
            logger.info("录制结束")
            launch { danmakuRecordingTask!!.stopRecordingAsync() }
        }
    }

    private fun removeIllegalChar(str: String): String {
        return str.replace("[\\\\/:*?\"<>|]".toRegex(), " ")
    }
}