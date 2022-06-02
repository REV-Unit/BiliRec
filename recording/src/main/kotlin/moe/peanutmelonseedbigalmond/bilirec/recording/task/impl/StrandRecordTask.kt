package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import kotlinx.coroutines.*
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileClosedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordFileOpenedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.extension.getCodecItemInStreamUrl
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import org.greenrobot.eventbus.EventBus
import java.io.InputStream
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext

class StrandRecordTask(
    private val room: Room,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BaseRecordTask(room), CoroutineScope by CoroutineScope(coroutineContext) {
    private val qnMap = mapOf(
        20000 to "4K",
        10000 to "原画",
        401 to "蓝光（杜比）",
        400 to "蓝光",
        250 to "超清",
        150 to "高清",
        80 to "流畅"
    )
    private val startAndStopLock = ReentrantLock()

    @Volatile
    private var started = false

    @Volatile
    private var mClosed = false
    override val closed: Boolean = mClosed

    @Volatile
    private var repairContext: LiveStreamRepairContext? = null

    @Volatile
    private lateinit var liveStream: InputStream

    override fun prepare() {
        // 准备时不需要做任何事
    }

    private suspend fun createLiveStreamRepairContextAsync(requireDelay: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                if (requireDelay) delay(1000)
                val (fullUrl, _) = getLiveStreamAddressAsync()
                liveStream = getLiveStreamAsync(fullUrl, Duration.ofMinutes(1))
            } catch (e: Exception) {
                logger.error("获取直播流出错：${e.stackTraceToString()}")
                logger.info("重新获取直播流")
                createLiveStreamRepairContextAsync(true)
            }
        }
    }

    override fun startAsync(baseFileName: String) {
        startAndStopLock.lock()
        if (started) {
            startAndStopLock.unlock()
            return
        }
        runBlocking{ createLiveStreamRepairContextAsync() }
        repairContext = LiveStreamRepairContext(liveStream, room, baseFileName)
        repairContext!!.startAsync()
        started = true
        EventBus.getDefault().post(RecordFileOpenedEvent(this.room.roomConfig.roomId, baseFileName))
        startAndStopLock.unlock()
    }

    override fun stopRecording() {
        startAndStopLock.lock()
        if (!started) {
            startAndStopLock.unlock()
            return
        }
        started = false
        logger.info("停止接收直播流")
        repairContext?.close()
        repairContext = null
        EventBus.getDefault().post(RecordFileClosedEvent(this.room.roomConfig.roomId))
        startAndStopLock.unlock()
    }

    override fun close() {
        startAndStopLock.lock()
        if (closed) {
            startAndStopLock.unlock()
            return
        }
        mClosed = true
        stopRecording()
        cancel()
        startAndStopLock.unlock()
    }

    // region 获取直播流
    private fun getLiveStreamAddressAsync(qn: Int = 10000): Pair<String, Int> {
        val selectedQn: Int
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