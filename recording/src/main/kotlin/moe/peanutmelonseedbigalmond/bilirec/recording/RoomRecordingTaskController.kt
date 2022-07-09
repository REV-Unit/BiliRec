package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.peanutmelonseedbigalmond.bilirec.closeQuietlyAsync
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.RecordTaskFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.DanmakuRecordTask
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
) : SuspendableCloseable {
    private val logger = LoggingFactory.getLogger(this.room.roomConfig.roomId, this)
    private val startAndStopLock = Mutex()
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    @Volatile
    private var videoRecordingTask: BaseRecordTask? = null
    private var danmakuRecordingTask: BaseRecordTask? = null

    @Volatile
    var started = false
        private set

    @Volatile
    private var closed = false

    suspend fun prepare() = withContext(scope.coroutineContext) {
        EventBus.getDefault().register(this@RoomRecordingTaskController)
        if (danmakuRecordingTask != null) {
            danmakuRecordingTask!!.close()
            danmakuRecordingTask = null
        }
        danmakuRecordingTask = DanmakuRecordTask(this@RoomRecordingTaskController.room, coroutineContext)
        danmakuRecordingTask!!.prepare()
        if (this@RoomRecordingTaskController.room.roomConfig.enableAutoRecord) {
            videoRecordingTask = RecordTaskFactory.getRecordTask(
                this@RoomRecordingTaskController.room,
                this@RoomRecordingTaskController.scope.coroutineContext
            )
            videoRecordingTask!!.prepare()
        }
    }

    override suspend fun close() {
        startAndStopLock.withLock {
            if (closed) return@withLock
            scope.cancel()
            requestStop()
            videoRecordingTask?.closeQuietlyAsync()
            danmakuRecordingTask!!.closeQuietlyAsync()
            videoRecordingTask = null
            danmakuRecordingTask = null
        }
        EventBus.getDefault().unregister(this@RoomRecordingTaskController)
    }

    suspend fun requestStop() {
        startAndStopLock.withLock {
            if (!started) return@withLock
            videoRecordingTask?.stopRecording()
            danmakuRecordingTask!!.stopRecording()
            started = false
        }
    }

    suspend fun requestStart() = withContext(scope.coroutineContext) {
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
            videoRecordingTask!!.start(baseFile.canonicalPath)
            started = true
        }
    }

    // 等待录制的视频文件创建了之后再启动弹幕录制
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileOpened(e: RecordFileOpenedEvent) {
        if (e.roomId == this.room.roomConfig.roomId) {
            logger.info("新建录制文件：${e.baseFileName}")
            scope.launch { danmakuRecordingTask!!.start(e.baseFileName) }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileClosed(e: RecordFileClosedEvent) {
        if (e.roomId == this.room.roomConfig.roomId) {
            logger.info("录制结束")
            scope.launch { danmakuRecordingTask!!.stopRecording() }
        }
    }

    private fun removeIllegalChar(str: String): String {
        return str.replace("[\\\\/:*?\"<>|]".toRegex(), " ")
    }
}