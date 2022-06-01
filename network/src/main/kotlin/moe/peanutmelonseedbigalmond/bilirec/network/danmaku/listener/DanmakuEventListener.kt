package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.listener

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.client.DanmakuTcpClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuModel
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuCommand


abstract class DanmakuEventListener {
    open fun onConnect(client: DanmakuTcpClient, roomId: Long) {}
    open fun onDisconnecting(client: DanmakuTcpClient, roomId: Long, code: Int, reason: String) {}
    open fun onDisconnected(client: DanmakuTcpClient, roomId: Long, code: Int, reason: String) {}
    open fun onDisconnectManually(client: DanmakuTcpClient, roomId: Long) {}
    open fun onError(client: DanmakuTcpClient, roomId: Long, error: Throwable) {}
    open fun onLiveStart(client: DanmakuTcpClient, roomId: Long) {}
    open fun onLiveEnd(client: DanmakuTcpClient, roomId: Long) {}
    open fun onDanmakuReceived(client: DanmakuTcpClient, roomId: Long, model: DanmakuModel) {}
    open fun onSendGift(client: DanmakuTcpClient, roomId: Long, model: DanmakuModel) {}
    open fun onSendSuperChat(client: DanmakuTcpClient, roomId: Long, model: DanmakuModel) {}
    open fun onGuardBuy(client: DanmakuTcpClient, roomId: Long, model: DanmakuModel) {}
    open fun onRoomChange(client: DanmakuTcpClient, roomId: Long, model: DanmakuModel) {}

    /**
     * **除[DanmakuCommand.LIVE_START]和[DanmakuCommand.LIVE_END]以外的**所有类型的事件都会触发此回调
     * @return Boolean
     * 当返回值为 true 时，表示消息已经被消费，不会触发其余回调
     */
    abstract fun onAllDanmakuReceived(client: DanmakuTcpClient, roomId: Long, model: DanmakuModel):Boolean

    open fun onOthersDanmakuReceived(client: DanmakuTcpClient, roomId: Long, model: DanmakuModel){}
}