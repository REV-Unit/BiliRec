package moe.peanutmelonseedbigalmond.bilirec.network.api.response

import com.google.gson.annotations.SerializedName

data class RoomInfoResponse(
    val uid:Long,
    @SerializedName("room_id")
    val roomId:Long,
    @SerializedName("short_id")
    val shortRoomId:Long,
    val title:String,
    @SerializedName("live_status")
    val liveStatus:Int,
    @SerializedName("area_name")
    val areaName:String,
    @SerializedName("parent_area_name")
    val parentAreaName:String,
    @SerializedName("parent_area_id")
    val parentAreaId:Long,
)
