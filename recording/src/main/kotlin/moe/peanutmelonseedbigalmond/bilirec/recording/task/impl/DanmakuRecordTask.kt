package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
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

class DanmakuRecordTask(
    private val room: Room,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BaseRecordTask(room), CoroutineScope by CoroutineScope(coroutineContext) {
    @Volatile
    private var danmakuWriter: DanmakuWriter? = null
    private var writeLock = Object()
    private lateinit var danmakuClient: DanmakuTcpClient

    @Volatile
    private var recording = false
    override val closed: Boolean = recording

    override fun prepare() {
        launch {
            EventBus.getDefault().register(this@DanmakuRecordTask)
            danmakuClient = DanmakuTcpClient(this@DanmakuRecordTask.room.roomConfig.roomId)
            connectToDanmakuServerAsync()
        }
    }

    override fun startAsync(baseFileName: String) {
        launch {
            synchronized(writeLock) {
                if (danmakuWriter != null) return@launch
                danmakuWriter = DanmakuWriter(
                    room = this@DanmakuRecordTask.room,
                    outputFileName = "$baseFileName.xml"
                )
                recording = true
            }
        }
    }

    override fun stopRecording() {
        runBlocking {
            synchronized(writeLock) {
                if (!recording) return@runBlocking
                if (danmakuWriter == null) return@runBlocking
                recording = false
                danmakuWriter!!.close()
                danmakuWriter = null
            }
        }
    }

    override fun close() {
        stopRecording()
        EventBus.getDefault().unregister(this)
        cancel()
    }

    private suspend fun connectToDanmakuServerAsync(requireDelay: Boolean = false): Unit =
        withContext(Dispatchers.IO) {
            if (closed) return@withContext
            try {
                if (requireDelay) delay(5000)
                danmakuClient.connectAsync()
            } catch (e: Exception) {
                logger.error("连接弹幕服务器出错：${e.stackTraceToString()}")
                connectToDanmakuServerAsync(true)
            }
        }

    // region 处理弹幕消息
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onDanmakuReceived(events: DanmakuEvents.DanmakuReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            synchronized(writeLock) {
                if (recording) {
                    logger.debug("收到弹幕：${events.danmakuModel.username} -> ${events.danmakuModel.commentText} (messageType=${events.danmakuModel.messageType}, lotteryDanmaku=${events.danmakuModel.lotteryDanmaku})")
                    // 过滤掉抽奖弹幕
                    if (room.roomConfig.filterLotteryDanmaku && events.danmakuModel.lotteryDanmaku) return
                    for (filterRegex in this.room.roomConfig.danmakuFilterRegex) {
                        try {
                            if (Regex(filterRegex).containsMatchIn(events.danmakuModel.commentText ?: "")) return
                        } catch (_: Exception) {
                            // 正则表达式解析出错，忽略
                        }
                    }
                    danmakuWriter?.writeDanmakuRecord(events.danmakuModel)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onGiftReceived(events: DanmakuEvents.GiftReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            synchronized(writeLock) {
                if (recording) {
                    logger.debug("收到礼物：${events.danmakuModel.username} -> ${events.danmakuModel.giftName} x ${events.danmakuModel.giftCount}")
                    danmakuWriter?.writeGiftRecord(events.danmakuModel)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onAnchorReceived(events: DanmakuEvents.AnchorReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            synchronized(writeLock) {
                if (recording) {
                    danmakuWriter?.writeGuardBuyRecord(events.danmakuModel)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSuperChatReceived(events: DanmakuEvents.SuperChatReceivedEvent) {
        if (events.roomId == this.room.roomConfig.roomId) {
            synchronized(writeLock) {
                if (recording) {
                    logger.trace("收到SuperChat：${events.danmakuModel.username} -> ${events.danmakuModel.commentText}")
                    danmakuWriter?.writeSuperChatRecord(events.danmakuModel)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
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
        launch {
            if (event.roomId == this@DanmakuRecordTask.room.roomConfig.roomId) {
                logger.info("弹幕服务器已断开，正在重连")
                connectToDanmakuServerAsync(true)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onWssClientError(event: DanmakuClientEvent.ClientFailure) {
        if (event.roomId == this.room.roomConfig.roomId) {
            logger.error("发生错误：${event.throwable.stackTraceToString()}")
        }
    }
    // endregion
}