package moe.peanutmelonseedbigalmond.bilirec.recording.task

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.extension.getCodecItemInStreamUrlAsync
import java.io.InputStream
import java.time.Duration
import kotlin.coroutines.coroutineContext

abstract class BaseVideoRecordTask(room: Room) : BaseRecordTask(room) {
    private val qnMap = mapOf(
        30000 to "杜比",
        20000 to "4K",
        10000 to "原画",
        401 to "蓝光（杜比）",
        400 to "蓝光",
        250 to "超清",
        150 to "高清",
        80 to "流畅"
    )

    @Volatile
    protected lateinit var liveStream: InputStream

    protected open suspend fun createLiveStreamRepairContext(requireDelay: Boolean = false) {
        if (requireDelay) delay(1000)
        val (fullUrl, _) = getLiveStreamAddress()
        liveStream = getLiveStream(fullUrl, Duration.ofSeconds(10))
    }

    // region 获取直播流
    private suspend fun getLiveStreamAddress(qn: Int = 10000): Pair<String, Int> {
        val selectedQn: Int
        val codecItemResp = BiliApiClient.DEFAULT_CLIENT.getCodecItemInStreamUrlAsync(room.roomConfig.roomId, qn)
        requireNotNull(codecItemResp) { "no supported stream url, qn: $qn" }

        if (!codecItemResp.acceptQn.contains(qn)) {
            logger.warn("返回直播流画质中不包含$qn（qn=${codecItemResp.acceptQn}），尝试使用其他画质")
            selectedQn = codecItemResp.acceptQn.maxOf { it } // 选择最高画质
            logger.warn("使用$selectedQn：${selectQnStr(selectedQn)}")
        }

        val urlInfos = codecItemResp.urlInfos
        require(!urlInfos.isNullOrEmpty()) { "no url_info" }

        val urlInfoWithoutMCDN = urlInfos.filter { !it.host.contains(".mcdn") }
        val urlInfo = if (urlInfoWithoutMCDN.isEmpty()) {
            urlInfoWithoutMCDN.random()
        } else {
            urlInfos.random()
        }

        val fullUrl = urlInfo.host + codecItemResp.baseUrl + urlInfo.extra
        logger.debug("获取直播流地址：$fullUrl")
        return Pair(fullUrl, codecItemResp.currentQn)
    }

    private suspend fun getLiveStream(fullUrl: String, readTimeout: Duration): InputStream {
        val resp = BiliApiClient.DEFAULT_CLIENT.getResponseAsync(fullUrl, readTimeout = readTimeout)
        if (resp.code == 200) {
            requireNotNull(resp.body)
            return resp.body!!.byteStream()
        }
        resp.close()
        throw Exception("get live stream failed, code: ${resp.code}")
    }

    private fun selectQnStr(qn: Int): String = qnMap[qn] ?: "未知"
    // endregion
}