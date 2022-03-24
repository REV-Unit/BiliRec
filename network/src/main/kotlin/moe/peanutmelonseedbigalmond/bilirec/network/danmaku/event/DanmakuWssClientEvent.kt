package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.DanmakuWssClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuModel

sealed class DanmakuWssClientEvent {
    data class ClientOpen(
        val client: DanmakuWssClient,
        val roomId: Long,
    ) : DanmakuWssClientEvent()

    data class ClientClosing(
        val client: DanmakuWssClient,
        val roomId:Long,
        val code:Int,
        val reason:String,
    ):DanmakuWssClientEvent()

    data class ClientClosed(
        val client: DanmakuWssClient,
        val roomId:Long,
        val code:Int,
        val reason:String,
    ):DanmakuWssClientEvent()

    data class ClientDisconnectManually(
        val client: DanmakuWssClient,
        val roomId: Long,
    ):DanmakuWssClientEvent()

    data class ClientFailure(
        val client: DanmakuWssClient,
        val roomId: Long,
        val throwable:Throwable,
    ):DanmakuWssClientEvent()
}