package moe.peanutmelonseedbigalmond.bilirec.network.api.response

import com.google.gson.annotations.SerializedName

data class LiveStreamUrlResponse(
    @SerializedName("live_status")
    val liveStatus:Int,
    val encrypted:Boolean,
    @SerializedName("playurl_info")
    val playUrlInfo: PlayUrlInfo?,
){
    data class PlayUrlInfo(
        @SerializedName("playurl")
        val playUrl: PlayUrl?,
    )

    data class PlayUrl(
        val stream: List<StreamItem>? = emptyList(),
    )
    data class StreamItem(
        @SerializedName("protocol_name")
        val protocolName:String,
        val format: List<FormatItem>? = emptyList(),
    )

    data class FormatItem(
        @SerializedName("format_name")
        val formatName:String,
        val codec: List<CodecItem>? = emptyList(),
    )

    data class CodecItem(
        @SerializedName("codec_name")
        val codecName:String,
        @SerializedName("base_url")
        val baseUrl:String,
        @SerializedName("current_qn")
        val currentQn:Int,
        @SerializedName("accept_qn")
        val acceptQn:List<Int> = emptyList(),
        @SerializedName("url_info")
        val urlInfos:List<UrlInfoItem>? = emptyList(),
    )

    data class UrlInfoItem(
        val host:String,
        val extra:String,
    )

}
