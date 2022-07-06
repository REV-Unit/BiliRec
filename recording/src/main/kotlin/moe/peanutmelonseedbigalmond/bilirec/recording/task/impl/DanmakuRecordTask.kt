package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.client.DanmakuTcpClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuClientEvent
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.writer.DanmakuWriter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class DanmakuRecordTask(
    room: Room,
    coroutineContext: CoroutineContext,
) : BaseRecordTask(room) {
    @Volatile
    private var danmakuWriter: DanmakuWriter? = null
    private var writeLock = Mutex()
    private lateinit var danmakuClient: DanmakuTcpClient
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    @Volatile
    private var recording = false
    override val closed: Boolean = recording

    override suspend fun prepare() = withContext(scope.coroutineContext) {
        EventBus.getDefault().register(this@DanmakuRecordTask)
        danmakuClient = DanmakuTcpClient(this@DanmakuRecordTask.room.roomConfig.roomId, scope.coroutineContext)
        connectToDanmakuServer()
    }

    override suspend fun start(baseFileName: String) = withContext(scope.coroutineContext) {
        writeLock.withLock {
            if (danmakuWriter != null) return@withLock
            danmakuWriter = DanmakuWriter(
                room = this@DanmakuRecordTask.room,
                outputFileName = "$baseFileName.xml"
            )
            recording = true
        }
    }

    override suspend fun stopRecording() = withContext(scope.coroutineContext) {
        writeLock.withLock {
            if (!recording) return@withLock
            if (danmakuWriter == null) return@withLock
            recording = false
            danmakuWriter!!.close()
            danmakuWriter = null
        }
    }

    override suspend fun close() {
        stopRecording()
        scope.cancel()
        EventBus.getDefault().unregister(this)
    }

    private suspend fun connectToDanmakuServer(requireDelay: Boolean = false): Unit =
        withContext(scope.coroutineContext) {
            if (closed) return@withContext
            return@withContext try {
                if (requireDelay) delay(5000)
                danmakuClient.connect()
            } catch (e: Exception) {
                logger.error("连接弹幕服务器出错：${e.localizedMessage}")
                logger.debug(e.stackTraceToString())
                connectToDanmakuServer(true)
            }
        }

    // region 处理弹幕消息
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onDanmakuReceived(events: DanmakuEvents.DanmakuReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            scope.launch {
                writeLock.withLock {
                    if (recording) {
                        logger.trace("收到弹幕：${events.danmakuModel.username} -> ${events.danmakuModel.commentText} (messageType=${events.danmakuModel.messageType}, lotteryDanmaku=${events.danmakuModel.lotteryDanmaku})")
                        // 过滤掉抽奖弹幕
                        if (room.roomConfig.filterLotteryDanmaku && events.danmakuModel.lotteryDanmaku) return@withLock
                        for (filterRegex in this@DanmakuRecordTask.room.roomConfig.danmakuFilterRegex) {
                            try {
                                if (Regex(filterRegex).containsMatchIn(
                                        events.danmakuModel.commentText ?: ""
                                    )
                                ) return@withLock
                            } catch (_: Exception) {
                                // 正则表达式解析出错，忽略
                            }
                        }
                        danmakuWriter?.writeDanmakuRecord(events.danmakuModel)
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onGiftReceived(events: DanmakuEvents.GiftReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            scope.launch {
                writeLock.withLock {
                    if (recording) {
                        logger.trace("收到礼物：${events.danmakuModel.username} -> ${events.danmakuModel.giftName} x ${events.danmakuModel.giftCount}")
                        danmakuWriter?.writeGiftRecord(events.danmakuModel)
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onAnchorReceived(events: DanmakuEvents.AnchorReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            scope.launch {
                writeLock.withLock {
                    if (recording) {
                        danmakuWriter?.writeGuardBuyRecord(events.danmakuModel)
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSuperChatReceived(events: DanmakuEvents.SuperChatReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            scope.launch {
                writeLock.withLock {
                    if (recording) {
                        logger.trace("收到SuperChat：${events.danmakuModel.username} -> ${events.danmakuModel.commentText}")
                        danmakuWriter?.writeSuperChatRecord(events.danmakuModel)
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Suppress("UNUSED_PARAMETER")
    fun onOtherEventReceived(event: DanmakuEvents.OtherEvent) {
        // Do nothing
    }
    // endregion

    // region 处理弹幕服务器状态
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientOpened(events: DanmakuClientEvent.ClientOpen) {
        if (events.roomId == this.room.roomConfig.roomId) {
            logger.info("已连接到弹幕服务器")
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientClosed(event: DanmakuClientEvent.ClientClosed) {
        if (event.roomId == this@DanmakuRecordTask.room.roomConfig.roomId) {
            logger.info("弹幕服务器已断开，正在重连")
            scope.launch { connectToDanmakuServer(true) }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientError(event: DanmakuClientEvent.ClientFailure) {
        if (event.roomId == this.room.roomConfig.roomId) {
            logger.error("发生错误：${event.throwable.localizedMessage}")
            logger.debug(event.throwable.stackTraceToString())
        }
    }
    // endregion
}