package moe.peanutmelonseedbigalmond.bilirec

data class RoomInfoRefreshEvent(
    val roomId: Long,
    val shortId: Long,
    val title: String
)