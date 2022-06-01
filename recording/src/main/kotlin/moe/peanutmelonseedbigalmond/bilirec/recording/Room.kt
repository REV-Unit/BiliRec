package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.*
import moe.peanutmelonseedbigalmond.bilirec.RoomInfoRefreshEvent
import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.time.OffsetDateTime
import kotlin.coroutines.CoroutineContext

class Room(
    val roomConfig: RoomConfig,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : Closeable, CoroutineScope by CoroutineScope(coroutineContext) {
    private val logger = LoggingFactory.getLogger(this.roomConfig.roomId, this)
    private val refreshInfoLock = Object()
    private lateinit var recordingTaskController: RoomRecordingTaskController

    // region 状态
    @Volatile
    var living = false
        private set

    @Volatile
    var recording = false
        private set

    @Volatile
    private var closed = false
    // endregion

    // region 房间信息
    @Volatile
    var shortId = 0L
        private set

    @Volatile
    lateinit var userName: String
        private set

    @Volatile
    lateinit var parentAreaName: String
        private set

    @Volatile
    lateinit var childAreaName: String
        private set

    @Volatile
    lateinit var title: String
        private set
    //endregion

    private lateinit var updateRoomInfoJob: Job

    fun prepareAsync(): Job {
        return launch(coroutineContext) {
            EventBus.getDefault().register(this@Room)
            updateRoomInfoJob = createUpdateRoomInfoJob()
            recordingTaskController = RoomRecordingTaskController(this@Room)
            recordingTaskController.prepareAsync()
            while (isActive) {
                try {
                    refreshRoomInfo()
                    logger.info("获取直播间信息成功：username=$userName, title=$title, parentAreaName=$parentAreaName, childAreaName=$childAreaName")
                    if (this@Room.living) requestStartAsync()
                    break
                }catch (_:CancellationException){

                } catch (e: Exception) {
                    logger.error("刷新房间信息失败，重试：$e")
                    logger.debug(e.stackTraceToString())
                    delay(1000)
                }
            }
            delay(5000)
            updateRoomInfoJob.start()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runBlocking(coroutineContext) { requestStop() }
        EventBus.getDefault().unregister(this)
        cancel()
    }

    // region start and stop
    private suspend fun requestStartAsync(forceStart: Boolean = false) {
        withContext(Dispatchers.IO) {
            // 没有在直播，直接返回
            if (!living) return@withContext
            // 如果既没有手动开始，配置文件也不是自动开始录制，则返回
            if (!forceStart && !this@Room.roomConfig.enableAutoRecord) return@withContext
            while (isActive) {
                try {
                    val startTime = OffsetDateTime.now()
                    val baseDir = File(removeIllegalChar("${this@Room.roomConfig.roomId}-${this@Room.userName}"))
                    if (!baseDir.exists()) baseDir.mkdirs()
                    val fileName = File(baseDir, removeIllegalChar(generateFileName(this@Room, startTime)))
                    recordingTaskController.requestStartAsync(fileName.canonicalPath)
                    this@Room.living = true
                    break
                } catch (e: Exception) {
                    logger.error("启动录制任务失败，1秒后重试")
                    logger.debug(e.stackTraceToString())
                    delay(1000)
                }
            }
        }
    }

    private suspend fun requestStop() {
        recordingTaskController.requestStopAsync()
    }
    // endregion

    // region 获取信息
    private fun refreshRoomInfo() {
        synchronized(refreshInfoLock) {
            getAnchorInfo()
            getRoomInfo()
        }
    }

    private fun getAnchorInfo() {
        val info = BiliApiClient.DEFAULT_CLIENT.getRoomAnchorInfo(this.roomConfig.roomId)
        this.userName = info.info.username
    }

    private fun getRoomInfo() {
        val roomInfo = BiliApiClient.DEFAULT_CLIENT.getRoomInfo(this.roomConfig.roomId)
        this.roomConfig.roomId = roomInfo.roomId
        this.shortId = roomInfo.shortRoomId
        this.parentAreaName = roomInfo.parentAreaName
        this.childAreaName = roomInfo.areaName
        this.title = roomInfo.title
        this.roomConfig.title = roomInfo.title
        this.living = roomInfo.liveStatus == 1

        EventBus.getDefault().post(
            RoomInfoRefreshEvent(
                roomId = roomInfo.roomId,
                title = roomInfo.title,
                shortId = roomInfo.shortRoomId
            )
        )
    }
    // endregion

    // region 处理录制事件
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordingThreadError(event: RecordingThreadErrorEvent) {
        if (event.room.roomConfig.roomId == this.roomConfig.roomId) {
            when (event.extra) {
                is EOFException -> {}
                is Exception -> {
                    logger.error("直播流修复时出现异常：${event.extra.stackTraceToString()}")
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordingThreadExited(event: RecordingThreadExitedEvent) {
        if (event.room.roomConfig.roomId == this.roomConfig.roomId) {
            runBlocking(coroutineContext) { requestStop() }
            // 重试
            launch {
                try {
                    getRoomInfo()
                    if (this@Room.living) requestStartAsync()
                } catch (e: Exception) {
                    logger.error("重试启动直播流时出现异常：${e.stackTraceToString()}")
                }
            }
        }
    }
    // endregion

    // region 定时刷新直播间信息
    private fun createUpdateRoomInfoJob(): Job {
        return launch(context = coroutineContext, start = CoroutineStart.LAZY) {
            while (isActive) {
                try {
                    refreshRoomInfo()
                    if (living) requestStartAsync()
                    delay(60 * 1000)
                } catch (_: CancellationException) {

                } catch (e: Exception) {
                    logger.error("获取直播间信息时出现异常：${e.stackTraceToString()}")
                }
            }
        }
    }
    // endregion

    // region 处理弹幕服务器的消息
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onLiveStart(event: DanmakuEvents.LiveStartEvent) {
        if (event.roomId == this.roomConfig.roomId) {
            logger.info("直播开始")
            this.living = true
            try {
                getRoomInfo()
                launch { requestStartAsync() }
            } catch (e: Exception) {
                logger.debug("刷新直播间信息时出现异常：${e.stackTraceToString()}")
                launch { requestStartAsync() }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onLiveEnd(event: DanmakuEvents.LiveEndEvent) {
        if (event.roomId == this.roomConfig.roomId) {
            logger.info("直播结束")
            logger.debug(event.danmakuModel.toString())
            launch { requestStop() }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRoomChanged(events: DanmakuEvents.RoomChangeEvent) {
        if (events.roomId == this.roomConfig.roomId) {
            logger.debug("房间信息变更为：${events.danmakuModel}")
            this.title = events.danmakuModel.title!!
            this.roomConfig.title = events.danmakuModel.title!!
            this.parentAreaName = events.danmakuModel.parentAreaName!!
            this.childAreaName = events.danmakuModel.areaName!!
        }
    }
    //endregion

    private fun removeIllegalChar(str: String): String {
        return str.replace("[\\\\/:*?\"<>|]".toRegex(), " ")
    }
}