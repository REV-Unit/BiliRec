package moe.peanutmelonseedbigalmond.bilirec.network.api.response

import com.google.gson.annotations.SerializedName

data class RoomAnchorInfoResponse(
    val info: RoomAnchorInfoResponseInfo,
){
    data class RoomAnchorInfoResponseInfo(
        val uid:Long,
        @SerializedName("uname")
        val username:String,
    )
}
