package moe.peanutmelonseedbigalmond.bilirec.recording.extension

import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.network.api.response.LiveStreamUrlResponse

internal fun BiliApiClient.getCodecItemInStreamUrl(
    roomId: Long,
    qn: Int
): LiveStreamUrlResponse.CodecItem? {
    val resp = this.getFlvLiveStreamUrlInfo(roomId, qn)
    val urlData = resp.playUrlInfo?.playUrl?.stream
    requireNotNull(urlData)

    return urlData.firstOrNull { it.protocolName == "http_stream" }
        ?.format?.firstOrNull { it.formatName == "flv" }
        ?.codec?.firstOrNull { it.codecName == "avc" }
}