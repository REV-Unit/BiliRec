package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.listener

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.DanmakuWssClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuModel
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuCommand


abstract class DanmakuEventListener {
    open fun onConnect(client: DanmakuWssClient, roomId: Long) {}
    open fun onDisconnecting(client: DanmakuWssClient, roomId: Long, code: Int, reason: String) {}
    open fun onDisconnected(client: DanmakuWssClient, roomId: Long, code: Int, reason: String) {}
    open fun onDisconnectManually(client: DanmakuWssClient, roomId: Long) {}
    open fun onError(client: DanmakuWssClient, roomId: Long, error: Throwable) {}
    open fun onLiveStart(client: DanmakuWssClient, roomId: Long) {}
    open fun onLiveEnd(client: DanmakuWssClient, roomId: Long) {}
    open fun onDanmakuReceived(client: DanmakuWssClient, roomId: Long, model: DanmakuModel) {}
    open fun onSendGift(client: DanmakuWssClient, roomId: Long, model: DanmakuModel) {}
    open fun onSendSuperChat(client: DanmakuWssClient, roomId: Long, model: DanmakuModel) {}
    open fun onGuardBuy(client: DanmakuWssClient, roomId: Long, model: DanmakuModel) {}
    open fun onRoomChange(client: DanmakuWssClient, roomId: Long, model: DanmakuModel) {}

    /**
     * **除[DanmakuCommand.LIVE_START]和[DanmakuCommand.LIVE_END]以外的**所有类型的事件都会触发此回调
     * @return Boolean
     * 当返回值为 true 时，表示消息已经被消费，不会触发其余回调
     */
    abstract fun onAllDanmakuReceived(client: DanmakuWssClient, roomId: Long, model: DanmakuModel):Boolean

    open fun onOthersDanmakuReceived(client: DanmakuWssClient,roomId: Long,model: DanmakuModel){}
}