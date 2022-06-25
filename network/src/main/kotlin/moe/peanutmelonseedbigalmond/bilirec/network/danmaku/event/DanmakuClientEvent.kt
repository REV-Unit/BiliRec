package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.client.DanmakuTcpClient

sealed class DanmakuClientEvent {
    data class ClientOpen(
        val client: DanmakuTcpClient,
        val roomId: Long,
    ) : DanmakuClientEvent()

    data class ClientClosed(
        val client: DanmakuTcpClient,
        val roomId: Long,
    ) : DanmakuClientEvent()

    data class ClientFailure(
        val client: DanmakuTcpClient,
        val roomId: Long,
        val throwable: Throwable,
    ) : DanmakuClientEvent()
}