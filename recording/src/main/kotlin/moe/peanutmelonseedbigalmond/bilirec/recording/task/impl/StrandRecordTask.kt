package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.extension.getCodecItemInStreamUrl
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import java.io.InputStream
import java.time.Duration

class StrandRecordTask(private val room: Room, private val outputFileNamePrefix: String) : BaseRecordTask(room) {
    private val qnMap = mapOf(
        20000 to "4K",
        10000 to "原画",
        401 to "蓝光（杜比）",
        400 to "蓝光",
        250 to "超清",
        150 to "高清",
        80 to "流畅"
    )

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean = mClosed

    @Volatile
    private var repairContext: LiveStreamRepairContext? = null

    override fun start() {
        if (started) {
            return
        }
        val (fullUrl, _) = getLiveStreamAddressAsync()
        val inputStream = getLiveStreamAsync(fullUrl, Duration.ofMinutes(1))
        repairContext = LiveStreamRepairContext(inputStream, room, outputFileNamePrefix)
        repairContext!!.startAsync()
        started = true
    }

    override fun close() {
        if (closed) return
        mClosed = true
        logger.info("停止接收直播流")
        repairContext?.close()
        repairContext=null
    }

    // region 获取直播流
    private fun getLiveStreamAddressAsync(qn: Int = 10000): Pair<String, Int> {
        var selectedQn = qn
        val codecItemResp = BiliApiClient.DEFAULT_CLIENT.getCodecItemInStreamUrl(room.roomConfig.roomId, qn)
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

    private fun getLiveStreamAsync(fullUrl: String, timeout: Duration): InputStream {
        val resp = BiliApiClient.DEFAULT_CLIENT.getResponse(fullUrl, timeout)
        if (resp.code == 200) {
            requireNotNull(resp.body)
            logger.info("开始接收直播流")
            return resp.body!!.byteStream()
        }
        resp.close()
        throw Exception("get live stream failed, code: ${resp.code}")
    }

    private fun selectQnStr(qn: Int): String = qnMap[qn] ?: "未知"
    // endregion
}