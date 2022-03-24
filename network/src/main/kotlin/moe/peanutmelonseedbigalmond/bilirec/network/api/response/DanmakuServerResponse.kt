package moe.peanutmelonseedbigalmond.bilirec.network.api.response

import com.google.gson.annotations.SerializedName

data class DanmakuServerResponse(
    @SerializedName("host_list")
    val hostList:List<HostListItem>,
    val token:String,
){
    data class HostListItem(
        val host:String,
        val port:Int,
        @SerializedName("wss_port")
        val wssPort:Int,
        @SerializedName("ws_port")
        val wsPort: Int
    )
}