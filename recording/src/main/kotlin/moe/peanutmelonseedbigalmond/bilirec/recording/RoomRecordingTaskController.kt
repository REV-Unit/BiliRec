package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import moe.peanutmelonseedbigalmond.bilirec.coroutine.withReentrantLock
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseVideoRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.RecordTaskFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.DanmakuRecordTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.time.OffsetDateTime
import kotlin.coroutines.CoroutineContext

class RoomRecordingTaskController(
    private val room: Room,
    coroutineContext: CoroutineContext
) : SuspendableCloseable {
    private val logger = LoggingFactory.getLogger(this.room.roomConfig.roomId, this)
    private val startAndStopLock = Mutex()
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    @Volatile
    private var videoRecordingTask: BaseVideoRecordTask? = null
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
    }

    override suspend fun close() {
        startAndStopLock.withReentrantLock {
            if (closed) return@withReentrantLock
            closed = true
            requestStop()
            videoRecordingTask?.close()
            videoRecordingTask = null
            danmakuRecordingTask!!.close()
            danmakuRecordingTask = null
            scope.cancel()
        }
        EventBus.getDefault().unregister(this@RoomRecordingTaskController)
    }

    suspend fun requestStop() = startAndStopLock.withReentrantLock {
        withContext(scope.coroutineContext) {
            if (!started) return@withContext
            videoRecordingTask?.stopRecording()
            videoRecordingTask?.close()
            videoRecordingTask = null
            danmakuRecordingTask!!.stopRecording()
            started = false
        }
    }

    suspend fun requestStart() = startAndStopLock.withReentrantLock {
        withContext(scope.coroutineContext) {
            if (started) return@withContext
            if (!this@RoomRecordingTaskController.room.roomConfig.enableAutoRecord) return@withContext
            videoRecordingTask = RecordTaskFactory.getRecordTask(
                this@RoomRecordingTaskController.room,
                this@RoomRecordingTaskController.scope.coroutineContext
            )
            videoRecordingTask!!.prepare()
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