package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import moe.peanutmelonseedbigalmond.bilirec.recording.writer.DanmakuWriter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.OffsetDateTime

class DanmakuRecordTask(
    private val room: Room,
    private val startTime: OffsetDateTime,
    private val fileNamePrefix: String
) : BaseRecordTask(room) {
    @Volatile
    private var danmakuWriter: DanmakuWriter? = null
    private var writeLock = Object()

    @Volatile
    private var recording = false
    override val closed: Boolean = recording
    override fun start() {
        if (recording) return
        synchronized(writeLock) {
            EventBus.getDefault().register(this)
            if (this.danmakuWriter == null) {
                this.danmakuWriter = DanmakuWriter(room, startTime, "$fileNamePrefix.xml")
            }
            recording = true
        }
    }

    override fun close() {
        if (!recording) return
        synchronized(writeLock) {
            EventBus.getDefault().unregister(this)
            danmakuWriter?.close()
            this.danmakuWriter = null
            recording = false
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
                        }catch (e:Exception){
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
    fun onOtherEvents(events: DanmakuEvents.OtherEvent) {

    }
    // endregion
}