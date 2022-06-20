package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.client

import com.google.gson.Gson
import kotlinx.coroutines.*
import moe.peanutmelonseedbigalmond.bilirec.network.api.BiliApiClient
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.PackUtils
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuMessageData
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuOperationCode
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuClientEvent
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.event.DanmakuEvents
import org.greenrobot.eventbus.EventBus
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

// 采用TCP协议连接到服务器
class DanmakuTcpClient(
    private val roomId: Long,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : Closeable, CoroutineScope by CoroutineScope(coroutineContext) {
    private val lock = Object()
    private var socket: Socket? = null
    private var connected: Boolean = false
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val gson = Gson()
    private lateinit var sendHeartbeatJob: Job
    private lateinit var receiveMessageJob: Job
    private lateinit var danmakuAddress: String
    private var danmakuServerPort by Delegates.notNull<Int>()
    private lateinit var danmakuServerToken: String

    companion object {
        // 心跳包的间隔时间，默认为60秒
        private const val HEARTBEAT_INTERVAL = 60 * 1000
    }

    suspend fun connectAsync() {
        withContext(coroutineContext) {
            if (connected) return@withContext
            if (socket == null) {
                socket = Socket()
            }
            getDanmakuServerInfoAsync()
            // 设置socket的超时时间，在两倍心跳包长度的时间内必定能收到或者发送数据
            // 超过这个时间则认为网络超时
            socket?.soTimeout = HEARTBEAT_INTERVAL * 2 + 1000
            withContext(Dispatchers.IO) { socket?.connect(InetSocketAddress(danmakuAddress, danmakuServerPort), 5000) }
            this@DanmakuTcpClient.inputStream = withContext(Dispatchers.IO) { socket?.getInputStream() }
            this@DanmakuTcpClient.outputStream = withContext(Dispatchers.IO) { socket?.getOutputStream() }
            connected = true
            this@DanmakuTcpClient.receiveMessageJob = createHandleMessageJob()
            sendHelloAsync()
            sendHeartbeatJob = createSendHeartbeatJob()
            sendHeartbeatJob.start()
            EventBus.getDefault()
                .post(DanmakuClientEvent.ClientOpen(this@DanmakuTcpClient, this@DanmakuTcpClient.roomId))
        }
    }

    private fun disconnect() {
        synchronized(lock) {
            if (connected) {
                outputStream?.close()
                outputStream = null
                inputStream?.close()
                inputStream = null
                socket?.close()
                socket = null
                connected = false
                EventBus.getDefault().post(DanmakuClientEvent.ClientClosed(this, this.roomId))
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            disconnect()
            cancel()
        }
    }

    private suspend fun getDanmakuServerInfoAsync() = withContext(Dispatchers.IO) {
        val danmakuServerInfo =
            BiliApiClient.DEFAULT_CLIENT.getDanmakuServer(this@DanmakuTcpClient.roomId)
        val server = danmakuServerInfo.hostList.random()
        this@DanmakuTcpClient.danmakuAddress = server.host
        this@DanmakuTcpClient.danmakuServerToken = danmakuServerInfo.token
        this@DanmakuTcpClient.danmakuServerPort = server.port
    }

    private suspend fun sendMessageAsync(operationCode: DanmakuOperationCode, body: String = "") =
        withContext(Dispatchers.IO) {
            val message = DanmakuMessageData().also {
                val messageDataBody = body.toByteArray(Charsets.UTF_8)
                it.operationCode = operationCode.code
                it.packetLength = (16 + messageDataBody.size)
                it.body = messageDataBody
            }
            sendAsync(PackUtils.pack(message))
        }

    private suspend fun sendHelloAsync() = withContext(Dispatchers.IO) {
        val messageBody = mapOf(
            "uid" to 0,
            "roomid" to roomId,
            "protover" to 3,
            "platform" to "web",
            "clientver" to "2.6.25",
            "type" to 2,
            "key" to danmakuServerToken,
        )
        sendMessageAsync(DanmakuOperationCode.ENTER_ROOM, gson.toJson(messageBody))
    }

    private suspend fun sendHeartBeatAsync() =
        sendMessageAsync(DanmakuOperationCode.HEART_BEAT)

    private suspend fun sendAsync(messageBody: ByteArray) {
        withContext(Dispatchers.IO) {
            if (connected) {
                outputStream?.write(messageBody)
                outputStream?.flush()
            }
        }
    }

    private fun dispatchMessage(messageBody: ByteArray) {
        val message = PackUtils.unpack(messageBody)
        for (m in message) {
            val event = DanmakuEvents.parse(this, this.roomId, m)
            EventBus.getDefault().post(event)
        }
    }

    private fun createSendHeartbeatJob(coroutineContext: CoroutineContext = Dispatchers.IO): Job {
        return launch(start = CoroutineStart.LAZY, context = coroutineContext) {
            while (isActive) {
                sendHeartBeatAsync()
                delay(HEARTBEAT_INTERVAL.toLong())
            }
        }
    }

    private fun createHandleMessageJob(
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): Job {
        return launch(coroutineContext) {
            try {
                while (this.isActive && this@DanmakuTcpClient.inputStream != null) {
                    val lengthByte =
                        withContext(Dispatchers.IO) { this@DanmakuTcpClient.inputStream!!.readNBytes(4) }
                    val length = lengthByte.toInt()
                    if (length == 0) continue
                    if (length == -1) {
                        this.cancel()
                        break
                    }
                    val body =
                        withContext(Dispatchers.IO) { this@DanmakuTcpClient.inputStream!!.readNBytes(length - 4) }
                    if (length < 4) continue
                    dispatchMessage(lengthByte + body)
                }
            } catch (_: CancellationException) {

            } catch (e: Exception) {
                EventBus.getDefault()
                    .post(
                        DanmakuClientEvent.ClientFailure(
                            this@DanmakuTcpClient,
                            this@DanmakuTcpClient.roomId,
                            e
                        )
                    )
                this@DanmakuTcpClient.disconnect()
            }
        }
    }

    private fun ByteArray.toInt(): Int =
        this[0].toInt().and(0xff).shl(24).or(
            this[1].toInt().and(0xff).shl(16).or(
                this[2].toInt().and(0xff).shl(8).or(
                    this[3].toInt().and(0xff)
                )
            )
        )
}