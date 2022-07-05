package moe.peanutmelonseedbigalmond.bilirec.events

data class RoomInfoRefreshEvent(
    val roomId: Long,
    val shortId: Long,
    val title: String
)