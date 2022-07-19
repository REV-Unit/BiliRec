package moe.peanutmelonseedbigalmond.bilirec.recording

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig
import moe.peanutmelonseedbigalmond.bilirec.coroutine.withReentrantLock
import moe.peanutmelonseedbigalmond.bilirec.events.RoomInfoRefreshEvent
import moe.peanutmelonseedbigalmond.bilirec.interfaces.SuspendableCloseable
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.client.DanmakuTcpClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuClientEvent
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseVideoRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.task.RecordTaskFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.writer.DanmakuWriter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.time.OffsetDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class Room(
    val roomConfig: RoomConfig,
    coroutineContext: CoroutineContext
) : SuspendableCloseable {
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    // 在开始和停止的时候都要求先获取这个锁才能操作
    private val startAndStopLock = Mutex()

    private val recordRetryLock = Mutex()

    // region 状态
    var living by Delegates.observable(false) { _, _, newValue ->
        if (newValue && enabledAutoRecord && autoRestart) {
            scope.launch { createRecordTaskAndStart() }
        }
    }
        private set

    @Volatile
    private var closed = false

    @Volatile
    private var danmakuClientConnected = false

    @Volatile
    var danmakuClientLastConnected = 0L
    // endregion

    // region 房间配置
    var enabledAutoRecord by Delegates.observable(roomConfig.enableAutoRecord) { _, _, newValue ->
        roomConfig.enableAutoRecord = newValue
    }
        private set

    // 是否自动重新启动录制
    @Volatile
    private var autoRestart = true
    // endregion

    // region 房间信息
    @Volatile
    var shortId = 0L
        private set

    var roomId: Long by Delegates.observable(roomConfig.roomId) { _, _, newValue ->
        roomConfig.roomId = newValue
        logger = LoggingFactory.getLogger(newValue, this@Room)
    }
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

    private var logger = LoggingFactory.getLogger(this.roomId, this)

    private lateinit var danmakuClient: DanmakuTcpClient
    private var danmakuWriter: DanmakuWriter? = null
    private var videoRecordTask: BaseVideoRecordTask? = null

    private var updateRoomInfoJob: Job? = null

    init {
        EventBus.getDefault().register(this)
        scope.launch {
            danmakuClient = DanmakuTcpClient(this@Room.roomId, scope.coroutineContext)
            refreshRoomInfo()
            launch { connectToDanmakuServer(false) }
            updateRoomInfoJob = createUpdateRoomInfoJob()
            updateRoomInfoJob!!.start()
        }
    }

    override suspend fun close() {
        if (closed) return
        requestStop()
        updateRoomInfoJob?.cancelAndJoin()
        updateRoomInfoJob = null
        this@Room.scope.cancel()
        closed = true
        EventBus.getDefault().unregister(this@Room)
    }

    // region start and stop
    private suspend fun createRecordTaskAndStart() {
        startAndStopLock.withReentrantLock {
            if (closed) return@withReentrantLock
            if (!living) return@withReentrantLock
            if (videoRecordTask != null) return@withReentrantLock

            this@Room.videoRecordTask = RecordTaskFactory.getRecordTask(this@Room, this@Room.scope.coroutineContext)
            scope.launch {
                try {
                    videoRecordTask!!.start(generateFileNameAndMkdir())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("启动录制出错: ${e.localizedMessage ?: e}")
                    logger.debug(e.stackTraceToString())
                    this@Room.videoRecordTask = null
                    launch { retryAfterRecordTaskFailed() }
                }
            }
        }
    }

    private suspend fun retryAfterRecordTaskFailed() {
        if (closed) return
        if (!this.living || !this.autoRestart) return
        if (!recordRetryLock.tryLock()) return
        try {
            delay(5000)
            if (!this.living || !this.autoRestart) return

            getRoomInfo()

            if (this.living && this.autoRestart) {
                createRecordTaskAndStart()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("重试开始录制时出错: ${e.localizedMessage ?: e}")
            logger.debug(e.localizedMessage)
            scope.launch { retryAfterRecordTaskFailed() }
        } finally {
            recordRetryLock.unlock()
        }
    }

    private suspend fun requestStop() {
        if (closed) return
        startAndStopLock.withReentrantLock {
            autoRestart = false

            if (videoRecordTask == null) return@withReentrantLock

            videoRecordTask?.close()
            videoRecordTask = null
        }
    }
    // endregion

    private suspend fun connectToDanmakuServer(delay: Boolean = true) {
        if (closed) return
        try {
            if (delay) {
                delay(10 * 1000)
            }
            danmakuClient.connect()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("连接到弹幕服务器出错: ${e.localizedMessage ?: e}")
            logger.debug(e.stackTraceToString())
            scope.launch { connectToDanmakuServer() }
        }
    }

    private fun generateFileNameAndMkdir(): String {
        val startTime = OffsetDateTime.now()
        val baseDir = File(
            removeIllegalChar(
                "${this.roomId}-${this.userName}"
            )
        )
        if (!baseDir.exists()) baseDir.mkdirs()
        return File(baseDir, removeIllegalChar(generateFileName(this, startTime))).canonicalPath
    }

    private fun removeIllegalChar(str: String): String {
        return str.replace("[\\\\/:*?\"<>|]".toRegex(), " ")
    }

    // region 获取信息
    private suspend fun refreshRoomInfo() {
        if (closed) return
        try {
            getRoomInfo()
            if (this.living && this.enabledAutoRecord && this.autoRestart) {
                createRecordTaskAndStart()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("刷新房间信息出错: ${e.localizedMessage ?: e}")
            logger.debug(e.stackTraceToString())
        }
    }

    private suspend fun getRoomInfo() {
        val info = BiliApiClient.DEFAULT_CLIENT.getRoomAnchorInfo(this@Room.roomConfig.roomId)
        this@Room.userName = info.info.username
        val roomInfo = BiliApiClient.DEFAULT_CLIENT.getRoomInfo(this@Room.roomConfig.roomId)
        this@Room.roomConfig.roomId = roomInfo.roomId
        this@Room.roomId = roomInfo.roomId
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
            if (event.extra is Exception) {
                logger.error("直播流修复时出现异常：${event.extra.localizedMessage ?: event.extra}")
                logger.debug(event.extra.stackTraceToString())
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordingThreadExited(event: RecordingThreadExitedEvent) {
        if (event.room.roomConfig.roomId == this.roomConfig.roomId) {
            runBlocking {
                videoRecordTask?.close()
                videoRecordTask = null
            }
            if (!scope.isActive) return
            scope.launch {
                try {
                    getRoomInfo()
                    if (living && autoRestart) {
                        launch { createRecordTaskAndStart() }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("重试开始录制时出错: ${e.localizedMessage ?: e}")
                    logger.debug(e.stackTraceToString())
                    launch { retryAfterRecordTaskFailed() }
                }
            }
        }
    }
    // endregion

    // region 定时刷新直播间信息
    private fun createUpdateRoomInfoJob(): Job {
        return scope.launch(start = CoroutineStart.LAZY) {
            while (isActive){
                launch { connectToDanmakuServer() }
                try {
                    getRoomInfo()
                    if (this@Room.living && this@Room.enabledAutoRecord && this@Room.autoRestart) {
                        launch { createRecordTaskAndStart() }
                    }
                    delay(60 * 1000)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.debug("获取直播间信息时出现异常：${e.localizedMessage}")
                    logger.debug(e.stackTraceToString())
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
            living = true
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onLiveEnd(event: DanmakuEvents.LiveEndEvent) {
        if (event.roomId == this.roomConfig.roomId) {
            logger.info("直播结束")
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

    // region 处理弹幕服务器状态
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onDanmakuClientConnected(event: DanmakuClientEvent.ClientOpen) {
        if (event.roomId == this.roomId) {
            logger.info("已连接到弹幕服务器")
            danmakuClientConnected = true
            danmakuClientLastConnected = System.currentTimeMillis()
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onDanmakuClientClosed(event: DanmakuClientEvent.ClientClosed) {
        if (event.roomId == this.roomId) {
            logger.debug("Danmaku client closed")
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onDanmakuClientFailure(event: DanmakuClientEvent.ClientFailure) {
        if (event.roomId == this.roomId) {
            val th = event.throwable
            logger.error("与弹幕服务器的通信过程中出现错误: ${th.localizedMessage ?: th}")
            logger.debug(th.stackTraceToString())
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onDanmakuClientDisconnected(event: DanmakuClientEvent.ClientDisconnected) {
        if (event.roomId == this.roomId) {
            scope.launch {
                logger.info("与弹幕服务器的连接被断开")
                danmakuClientConnected = false

                // 如果上次连上的时间距离现在超过 1 分钟
                launch { connectToDanmakuServer(System.currentTimeMillis() - danmakuClientLastConnected >= 60 * 1000) }
                danmakuClientLastConnected = Long.MAX_VALUE
            }
        }
    }
    // endregion

    // region 处理录制状态
    // 等待 flv 文件开始写入磁盘之后，再创建弹幕 xml
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileOpened(event: RecordFileOpenedEvent) {
        if (event.roomId == this.roomId) {
            danmakuWriter = DanmakuWriter(this, event.baseFileName)
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onRecordFileClosed(event: RecordFileClosedEvent) {
        if (event.roomId == this.roomId) {
            this.danmakuWriter?.close()
            this.danmakuWriter = null
        }
    }
    // endregion

    // region 处理弹幕消息
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onDanmakuReceived(events: DanmakuEvents.DanmakuReceivedEvent) {
        if (events.roomId == this.roomId) {
            scope.launch {
                logger.trace("收到弹幕：${events.danmakuModel.username} -> ${events.danmakuModel.commentText} (messageType=${events.danmakuModel.messageType}, lotteryDanmaku=${events.danmakuModel.lotteryDanmaku})")
                // 过滤掉抽奖弹幕
                if (roomConfig.filterLotteryDanmaku && events.danmakuModel.lotteryDanmaku) return@launch
                for (filterRegex in this@Room.roomConfig.danmakuFilterRegex) {
                    try {
                        if (Regex(filterRegex).containsMatchIn(events.danmakuModel.commentText ?: "")) return@launch
                    } catch (_: Exception) {
                        // 正则表达式解析出错，忽略
                    }
                }
                withContext(Dispatchers.IO) { danmakuWriter?.writeDanmakuRecord(events.danmakuModel) }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onGiftReceived(events: DanmakuEvents.GiftReceivedEvent) {
        if (events.roomId == this.roomConfig.roomId) {
            scope.launch {
                logger.trace("收到礼物：${events.danmakuModel.username} -> ${events.danmakuModel.giftName} x ${events.danmakuModel.giftCount}")
                withContext(Dispatchers.IO) { danmakuWriter?.writeGiftRecord(events.danmakuModel) }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onAnchorReceived(events: DanmakuEvents.AnchorReceivedEvent) {
        if (events.roomId == this.roomConfig.roomId) {
            scope.launch {
                withContext(Dispatchers.IO) { danmakuWriter?.writeGuardBuyRecord(events.danmakuModel) }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSuperChatReceived(events: DanmakuEvents.SuperChatReceivedEvent) {
        if (events.roomId == this.roomConfig.roomId) {
            scope.launch {
                logger.trace("收到SuperChat：${events.danmakuModel.username} -> ${events.danmakuModel.commentText}")
                withContext(Dispatchers.IO) { danmakuWriter?.writeSuperChatRecord(events.danmakuModel) }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Suppress("UNUSED_PARAMETER")
    fun onOtherEventReceived(event: DanmakuEvents.OtherEvent) {
        // Do nothing
    }
// endregion
}