package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.DanmakuWssClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuModel
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuCommand.*

open class DanmakuEvents private constructor(
    open val client: DanmakuWssClient,
    open val roomId: Long,
    open val danmakuModel: DanmakuModel
) {
    data class DanmakuReceivedEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)

    data class GiftReceivedEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)

    data class SuperChatReceivedEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)

    data class AnchorReceivedEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)

    data class RoomChangeEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)

    data class LiveStartEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)

    data class LiveEndEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)

    data class OtherEvent(
        override val client: DanmakuWssClient,
        override val roomId: Long,
        override val danmakuModel: DanmakuModel,
    ) : DanmakuEvents(client, roomId, danmakuModel)


    companion object {
        fun parse(
            client: DanmakuWssClient,
            roomId: Long,
            danmakuModel: DanmakuModel,
        ): DanmakuEvents {
            return when (danmakuModel.command) {
                DANMAKU -> DanmakuReceivedEvent(client, roomId, danmakuModel)
                SEND_GIFT -> GiftReceivedEvent(client, roomId, danmakuModel)
                LIVE_END -> LiveEndEvent(client, roomId, danmakuModel)
                LIVE_START -> LiveStartEvent(client, roomId, danmakuModel)
                ROOM_CHANGE -> RoomChangeEvent(client, roomId, danmakuModel)
                SUPER_CHAT, SUPER_CHAT_MESSAGE_JPN -> SuperChatReceivedEvent(client, roomId, danmakuModel)
                GUARD_BUY -> AnchorReceivedEvent(client, roomId, danmakuModel)
                else -> OtherEvent(client, roomId, danmakuModel)
            }
        }
    }
}