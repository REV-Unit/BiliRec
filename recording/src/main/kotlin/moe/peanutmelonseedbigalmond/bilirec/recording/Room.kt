package moe.peanutmelonseedbigalmond.bilirec.recording

import moe.peanutmelonseedbigalmond.bilirec.RoomInfoRefreshEvent
import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.DanmakuWssClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuWssClientEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.RecordTaskFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.task.impl.DanmakuRecordTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.EOFException
import java.io.File
import java.time.OffsetDateTime
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.log

class Room(val roomConfig: RoomConfig) : AutoCloseable {
    private val logger = LoggingFactory.getLogger(this.roomConfig.roomId, this)
    private val timer = Timer("timer - ${this.roomConfig.roomId}")
    private val lock = Object()
    private val refreshInfoLock = Object()

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
    lateinit var danmakuServer: String
        private set

    @Volatile
    lateinit var danmakuToken: String
        private set

    @Volatile
    lateinit var title: String
        private set
    //endregion

    // region Timer任务
    private var updateRoomInfoTask: UpdateRoomInfoTask? = null
    // endregion

    private var danmakuClient: DanmakuWssClient? = null

    @Volatile
    private var clientClosedManually = false

    @Volatile
    private var recordTask: BaseRecordTask? = null

    @Volatile
    private var danmakuRecordTask: DanmakuRecordTask? = null

    init {
        EventBus.getDefault().register(this)
        thread {
            this.updateRoomInfoTask = UpdateRoomInfoTask()
            while (true) {
                try {
                    refreshRoomInfo()
                    logger.info("获取直播间信息成功：username=$userName, title=$title, parentAreaName=$parentAreaName, childAreaName=$childAreaName")
                    if (this.living) requestStart()
                    break
                } catch (e: Exception) {
                    logger.error("刷新房间信息失败，重试：$e")
                    logger.debug(e.stackTraceToString())
                    Thread.sleep(1000)
                }
            }
            timer.schedule(updateRoomInfoTask!!, 5000, 60000)
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            requestStop()
            timer.cancel()
            EventBus.getDefault().unregister(this)
        }
    }

    private fun createAndStartTask(startTime: OffsetDateTime, fileName: String) {
        if (!living) return
        if (recording) return
        if (this.recordTask == null) {
            this.recordTask = RecordTaskFactory.getRecordTask(this, fileName)
        }
        if (this.recordTask?.closed == false) {
            recordTask?.start()
        }
        if (this.danmakuRecordTask == null) {
            this.danmakuRecordTask = DanmakuRecordTask(this, startTime, fileName)
        }
        if (this.danmakuRecordTask?.closed == false) {
            danmakuRecordTask?.start()
        }
        danmakuClient?.requireSendDanmakuEvent = true
    }

    // region start and stop
    private fun requestStart(forceStart: Boolean = false) {
        if (this.roomConfig.enableAutoRecord || forceStart){
            thread {
                synchronized(lock) {
                    if (!living) return@thread
                    if (recording) return@thread
                    while (true) {
                        try {
                            val startTime = OffsetDateTime.now()
                            val baseDir = File(removeIllegalChar("${this.roomConfig.roomId}-${this.userName}"))
                            if (!baseDir.exists()) baseDir.mkdirs()
                            val fileName = File(baseDir, removeIllegalChar(generateFileName(this, startTime)))
                            createAndStartTask(startTime, fileName.canonicalPath)
                            this.living = true
                            break
                        } catch (e: Exception) {
                            this.recordTask?.close()
                            this.recordTask = null
                            this.recordTask?.close()
                            this.danmakuRecordTask = null
                            logger.error("启动录制任务失败，1秒后重试")
                            logger.debug(e.stackTraceToString())
                            Thread.sleep(1000)
                        }
                    }
                    recording = true
                }
            }
        }
    }

    private fun requestStop() {
        synchronized(lock) {
            if (!living || !recording) return
            danmakuClient?.requireSendDanmakuEvent = false
            danmakuClient = null
            this.danmakuRecordTask?.close()
            this.danmakuRecordTask = null
            this.recordTask?.close()
            this.recordTask = null
            this.recording = false
            this.living = false
        }
    }
    // endregion

    // region 获取信息
    private fun refreshRoomInfo() {
        synchronized(refreshInfoLock) {
            // 如果弹幕客户端已经存在，就没有必要重新连接
            if (this.danmakuClient == null) {
                getDanmakuServerInfo()
                this.danmakuClient = DanmakuWssClient(
                    this.danmakuServer,
                    this.roomConfig.roomId,
                    this.danmakuToken,
                )
                if (this@Room.danmakuClient?.closed == false) {
                    this.danmakuClient!!.connectAsync()
                }
            }
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

    private fun getDanmakuServerInfo() {
        val danmakuServerInfo = BiliApiClient.DEFAULT_CLIENT.getDanmakuServer(this.roomConfig.roomId)
        val server = danmakuServerInfo.hostList.random()
        this.danmakuServer = "wss://" + server.host + ":" + server.wssPort + "/sub"
        this.danmakuToken = danmakuServerInfo.token
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
            synchronized(lock) {
                requestStop()
                // 重试
                thread {
                    try {
                        getRoomInfo()
                        if (this.living) requestStart()
                    } catch (e: Exception) {
                        logger.error("重试启动直播流时出现异常：${e.stackTraceToString()}")
                    }
                }
            }
        }
    }
    // endregion

    // region 处理弹幕服务器状态
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientOpened(events: DanmakuWssClientEvent.ClientOpen) {
        if (events.roomId == this.roomConfig.roomId) {
            logger.info("已连接到弹幕服务器")
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientClosed(event: DanmakuWssClientEvent.ClientClosed) {
        synchronized(this.clientClosedManually) {
            if (!this.clientClosedManually) {
                logger.info("弹幕服务器已断开，5秒后重连")
                Thread.sleep(5000)
                event.client.connectAsync()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientClosedManually(event: DanmakuWssClientEvent.ClientDisconnectManually) {
        if (event.roomId == this.roomConfig.roomId) {
            synchronized(clientClosedManually) {
                this.clientClosedManually = true
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientError(event: DanmakuWssClientEvent.ClientFailure) {
        if (event.roomId == this.roomConfig.roomId) {
            logger.error("发生错误：${event.throwable.stackTraceToString()}")
            requestStop()
        }
    }
    // endregion

    // region 处理弹幕服务器的消息
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onLiveStart(event: DanmakuEvents.LiveStartEvent) {
        if (event.roomId == this.roomConfig.roomId) {
            logger.info("直播开始")
            logger.debug(event.danmakuModel.toString())
            try {
                getRoomInfo()
                if (this.living) requestStart()
            } catch (e: Exception) {
                logger.debug("刷新直播间信息时出现异常：${e.stackTraceToString()}")
                this.living = true
                requestStart()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onLiveEnd(event: DanmakuEvents.LiveEndEvent) {
        if (event.roomId == this.roomConfig.roomId) {
            logger.info("直播结束")
            logger.debug(event.danmakuModel.toString())
            requestStop()
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
    // endregion

    // region 定时刷新直播间信息
    private inner class UpdateRoomInfoTask : TimerTask() {
        override fun run() {
            try {
                refreshRoomInfo()
                if (living) requestStart()
            } catch (e: Exception) {
                logger.error("获取直播间信息时出现异常：${e.stackTraceToString()}")
            }
        }
    }
    // endregion

    private fun removeIllegalChar(str: String): String {
        return str.replace("[\\\\/:*?\"<>|]".toRegex(), " ")
    }
}