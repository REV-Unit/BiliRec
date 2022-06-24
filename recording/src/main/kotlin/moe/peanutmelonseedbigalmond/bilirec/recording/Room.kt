package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ObservableProperty

class Room(
    val roomConfig: RoomConfig,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : Closeable, CoroutineScope by CoroutineScope(coroutineContext) {
    private val logger = LoggingFactory.getLogger(this.roomConfig.roomId, this)
    private lateinit var recordingTaskController: RoomRecordingTaskController

    // 在开始和停止的时候都要求先获取这个锁才能操作
    private val startAndStopLock = ReentrantLock()

    // region 状态
    @Volatile
    var living = false
        private set(value) {
            if (value) {
                runBlocking { requestStartAsync() }
            } else {
                runBlocking { requestStopAsync() }
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

    fun prepareAsync(): Job {
        EventBus.getDefault().register(this)
        return launch(Dispatchers.IO) {
            updateRoomInfoJob = createUpdateRoomInfoJob()
            recordingTaskController = RoomRecordingTaskController(this@Room)
            recordingTaskController.prepareAsync()
            while (isActive) {
                try {
                    refreshRoomInfoAsync()
                    logger.info("获取直播间信息成功：username=$userName, title=$title, parentAreaName=$parentAreaName, childAreaName=$childAreaName, living=$living")
                    break
                } catch (_: CancellationException) {

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
        runBlocking {
            requestStopAsync()
            updateRoomInfoJob.cancelAndJoin()
        }
        cancel()
        EventBus.getDefault().unregister(this)
    }

    // region start and stop
    private suspend fun requestStartAsync() {
        coroutineScope {
            startAndStopLock.lock()
            if (closed) {
                startAndStopLock.unlock()
                return@coroutineScope
            }
            requireRestart = true
            while (isActive) {
                try {
                    recordingTaskController.requestStartAsync()
                    break
                } catch (e: Exception) {
                    logger.error("启动录制任务失败，1秒后重试")
                    logger.debug(e.stackTraceToString())
                    delay(1000)
                } finally {
                    startAndStopLock.unlock()
                }
            }
        }
    }

    private suspend fun requestStopAsync() {
        try {
            startAndStopLock.lock()
            requireRestart = false
            recordingTaskController.requestStopAsync()
        } finally {
            startAndStopLock.unlock()
        }
    }
    // endregion

    // region 获取信息
    private suspend fun refreshRoomInfoAsync() {
        getAnchorInfoAsync()
        getRoomInfoAsync()
    }

    private suspend fun getAnchorInfoAsync() {
        val info = BiliApiClient.DEFAULT_CLIENT.getRoomAnchorInfo(this.roomConfig.roomId)
        this.userName = info.info.username
    }

    private suspend fun getRoomInfoAsync() {
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
            runBlocking { this@Room.recordingTaskController.requestStopAsync() }
            if (!requireRestart) return
            // 重试
            launch {
                while (isActive) {
                    try {
                        getRoomInfoAsync()
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
    // endregion

    // region 定时刷新直播间信息
    private fun createUpdateRoomInfoJob(): Job {
        return launch(context = Dispatchers.IO, start = CoroutineStart.LAZY) {
            while (isActive) {
                try {
                    refreshRoomInfoAsync()
                } catch (_: CancellationException) {

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
                launch {
                    try {
                        getRoomInfoAsync()
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