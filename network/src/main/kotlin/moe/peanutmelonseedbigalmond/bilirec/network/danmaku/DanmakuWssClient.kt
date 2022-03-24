package moe.peanutmelonseedbigalmond.bilirec.network.danmaku

import com.google.gson.Gson
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuMessageData
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuOperationCode
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuWssClientEvent
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.greenrobot.eventbus.EventBus
import java.io.Closeable
import java.net.SocketException
import java.util.*

class DanmakuWssClient(
    private val danmakuWebsocketAddress: String,
    private val roomId: Long,
    private val token: String,
    private val client: OkHttpClient = DEFAULT_CLIENT
) : Closeable {
    companion object {
        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .build()
    }

    private val gson = Gson()
    private var timer = Timer()
    private var timerTask: TimerTask? = null
    private var ws: WebSocket? = null

    /**
     * 是否继续推送弹幕事件
     * **此项不包括直播开始，直播结束和直播间信息更改**
     */
    @Volatile
    var requireSendDanmakuEvent = false

    @Volatile
    var closed = false
        private set

    // region Tasks
    private inner class SendHeartbeatTask : TimerTask() {
        override fun run() = sendHeartBeat()
    }
    // endregion

    private val websocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            this@DanmakuWssClient.ws = webSocket
            sendHelloMessageAsync()
            this@DanmakuWssClient.timerTask = SendHeartbeatTask()
            this@DanmakuWssClient.timer.schedule(timerTask, 0, 1000 * 30)
            EventBus.getDefault().post(DanmakuWssClientEvent.ClientOpen(this@DanmakuWssClient, roomId))
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            EventBus.getDefault().post(DanmakuWssClientEvent.ClientClosing(this@DanmakuWssClient, roomId, code, reason))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            EventBus.getDefault().post(DanmakuWssClientEvent.ClientClosed(this@DanmakuWssClient, roomId, code, reason))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            EventBus.getDefault().post(DanmakuWssClientEvent.ClientFailure(this@DanmakuWssClient, roomId, t))
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            dispatchDanmakuMessage(bytes.toByteArray())
        }
    }

    fun connectAsync() {
        val request = Request.Builder()
            .url(danmakuWebsocketAddress)
            .addHeader("referer", "https://live.bilibili.com/$roomId")
            .addHeader(
                "user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36"
            )
            .build()
        client.newWebSocket(request, websocketListener)
    }

    private fun dispatchDanmakuMessage(danmakuByteArray: ByteArray) {
        val messageList = PackUtils.unpack(danmakuByteArray)
        for (message in messageList) {
            when (val event = DanmakuEvents.parse(this, this.roomId, message)) {
                is DanmakuEvents.LiveStartEvent,
                is DanmakuEvents.LiveEndEvent,
                is DanmakuEvents.RoomChangeEvent -> {
                    EventBus.getDefault().post(event)
                }
                else -> {
                    if (requireSendDanmakuEvent) {
                        EventBus.getDefault().post(event)
                    }
                }
            }
        }
    }

    // region Send message
    private fun sendHelloMessageAsync() {
        val messageBody = mapOf(
            "uid" to 0,
            "roomid" to roomId,
            "protover" to 3,
            "platform" to "web",
            "clientver" to "2.6.25",
            "type" to 2,
            "key" to token,
        )
        sendMessageAsync(ws!!, DanmakuOperationCode.ENTER_ROOM, gson.toJson(messageBody))
    }

    private fun sendHeartBeat() {
        sendMessageAsync(ws!!, DanmakuOperationCode.HEART_BEAT)
    }

    private fun sendMessageAsync(websocket: WebSocket, operationCode: DanmakuOperationCode, body: String = "") {
        val message = DanmakuMessageData().also {
            val messageDataBody = body.toByteArray(Charsets.UTF_8)
            it.operationCode = operationCode.code
            it.packetLength = (16 + messageDataBody.size)
            it.body = messageDataBody
        }
        websocket.send(PackUtils.pack(message).toByteString())
    }
    // endregion

    override fun close() {
        if (this.closed) return
        EventBus.getDefault().post(DanmakuWssClientEvent.ClientDisconnectManually(this, roomId))
        ws?.close(1000, "closed by client")
        timerTask?.cancel()
        timer.purge()
        timer.cancel()
        closed = true
    }
}