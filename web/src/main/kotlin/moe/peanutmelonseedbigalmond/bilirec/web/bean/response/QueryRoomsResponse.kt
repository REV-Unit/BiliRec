package moe.peanutmelonseedbigalmond.bilirec.web.bean.response

import moe.peanutmelonseedbigalmond.bilirec.recording.Room

data class QueryRoomsResponse(
    val data: List<RoomResponse>
) {
    data class RoomResponse(
        val roomId: Long,
        val shortId: Long,
        val userName: String,
        val parentAreaName: String,
        val childAreaName: String,
        val title: String,
        val living: Boolean,
    ) {
        companion object {
            fun fromRoom(room: Room): RoomResponse {
                return RoomResponse(
                    roomId = room.roomConfig.roomId,
                    shortId = room.shortId,
                    userName = room.userName,
                    parentAreaName = room.parentAreaName,
                    childAreaName = room.childAreaName,
                    title = room.title,
                    living = room.living
                )
            }
        }
    }
}