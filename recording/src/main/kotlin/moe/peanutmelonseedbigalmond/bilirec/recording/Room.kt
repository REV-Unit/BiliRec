package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.peanutmelonseedbigalmond.bilirec.events.RoomInfoRefreshEvent
import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class Room(
    val roomConfig: RoomConfig,
    coroutineContext: CoroutineContext
) : SuspendableCloseable {
    private val logger = LoggingFactory.getLogger(this.roomConfig.roomId, this)
    private lateinit var recordingTaskController: RoomRecordingTaskController
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    // 在开始和停止的时候都要求先获取这个锁才能操作
    private val startAndStopLock = Mutex()

    // region 状态
    @Volatile
    var living = false
        private set(value) {
            if (value) {
                runBlocking(scope.coroutineContext) { requestStart() }
            } else {
                runBlocking(scope.coroutineContext) { requestStop() }
            }
            field = value
        }

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

    @Volatile
    private var requireRestart = true
    //endregion

    private lateinit var updateRoomInfoJob: Job

    fun prepare(): Job {
        EventBus.getDefault().register(this)
        return scope.launch {
            updateRoomInfoJob = createUpdateRoomInfoJob()
            recordingTaskController = RoomRecordingTaskController(this@Room, coroutineContext)
            recordingTaskController.prepare()
            while (isActive) {
                try {
                    refreshRoomInfo()
                    logger.info("获取直播间信息成功：username=$userName, title=$title, parentAreaName=$parentAreaName, childAreaName=$childAreaName, living=$living")
                    break
                } catch (e: CancellationException) {
                    throw e
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

    override suspend fun close() {
        if (closed) return
        requestStop()
        updateRoomInfoJob.cancelAndJoin()
        this@Room.scope.cancel()
        closed = true
        EventBus.getDefault().unregister(this@Room)
    }

    // region start and stop
    private suspend fun requestStart() = withContext(scope.coroutineContext){
        startAndStopLock.withLock {
            if (closed) return@withLock
            requireRestart = true
            while (coroutineContext.isActive) {
                try {
                    recordingTaskController.requestStart()
                    break
                } catch (e: Exception) {
                    logger.error("启动录制任务失败，1秒后重试")
                    logger.debug(e.stackTraceToString())
                    delay(1000)
                }
            }
        }
    }

    private suspend fun requestStop() = withContext(scope.coroutineContext){
        startAndStopLock.withLock {
            requireRestart = false
            recordingTaskController.requestStop()
        }
    }
    // endregion

    // region 获取信息
    private suspend fun refreshRoomInfo() {
        getAnchorInfo()
        getRoomInfo()
    }

    private suspend fun getAnchorInfo() {
        val info = BiliApiClient.DEFAULT_CLIENT.getRoomAnchorInfo(this@Room.roomConfig.roomId)
        this@Room.userName = info.info.username
    }

    private suspend fun getRoomInfo() {
        val roomInfo = BiliApiClient.DEFAULT_CLIENT.getRoomInfo(this@Room.roomConfig.roomId)
        this@Room.roomConfig.roomId = roomInfo.roomId
        this@Room.shortId = roomInfo.shortRoomId
        this@Room.parentAreaName = roomInfo.parentAreaName
        this@Room.childAreaName = roomInfo.areaName
        this@Room.title = roomInfo.title
        this@Room.roomConfig.title = roomInfo.title
        this@Room.living = roomInfo.liveStatus == 1

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
                is Exception -> {
                    logger.error("直播流修复时出现异常：${event.extra.localizedMessage}")
                    logger.debug(event.extra.stackTraceToString())
                    this@Room.living = false
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordingThreadExited(event: RecordingThreadExitedEvent) {
        if (event.room.roomConfig.roomId == this.roomConfig.roomId) {
            runBlocking(scope.coroutineContext) {
                this@Room.recordingTaskController.requestStop()
                if (!requireRestart) return@runBlocking
                // 重试
                launch {
                    while (isActive) {
                        try {
                            getRoomInfo()
                            break
                        } catch (e: Exception) {
                            logger.error("重试启动直播流时出现异常：${e.localizedMessage}")
                            logger.debug(e.stackTraceToString())
                            delay(5000)
                        }
                    }
                }
            }
        }
    }
    // endregion

    // region 定时刷新直播间信息
    private fun createUpdateRoomInfoJob(): Job {
        return scope.launch(start = CoroutineStart.LAZY) {
            while (isActive) {
                try {
                    getRoomInfo()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("获取直播间信息时出现异常：${e.localizedMessage}")
                    logger.debug(e.stackTraceToString())
                } finally {
                    delay(60 * 1000)
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
            logger.debug(event.danmakuModel.toString())
            if (event.danmakuModel.liveTime == 0L) {
                scope.launch {
                    try {
                        getRoomInfo()
                    } catch (e: Exception) {
                        logger.debug("刷新直播间信息时出现异常：${e.stackTraceToString()}")
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onLiveEnd(event: DanmakuEvents.LiveEndEvent) {
        if (event.roomId == this.roomConfig.roomId) {
            logger.info("直播结束")
            logger.debug(event.danmakuModel.toString())
            this.living = false
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
}