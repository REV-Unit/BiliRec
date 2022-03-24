package moe.peanutmelonseedbigalmond.bilirec.network.danmaku

import com.google.gson.Gson
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuMessageData
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuOperationCode
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

// 采用TCP协议连接到服务器
class DanmakuTcpClient(
    private val danmakuAddress: String,
    private val danmakuPort: Int,
    private val roomId: Long,
    private val token: String,
) : AutoCloseable {
    private val lock = Object()
    private var socket: Socket? = null
    val connected: Boolean
        get() = socket?.isConnected ?: false
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val gson = Gson()
    private var timerTask: TimerTask? = null
    private val timer = Timer()
    private var receiveTask: ReceiveMessageThread? = null

    fun connectAsync() = thread {
        if (socket == null) {
            socket = Socket()
        }
        try {
            socket?.connect(InetSocketAddress(danmakuAddress, danmakuPort), 6000)
            println("TCP连接成功")
            this.inputStream = socket?.getInputStream()
            this.outputStream = socket?.getOutputStream()
            this.receiveTask = ReceiveMessageThread()
            thread(name = "DanmakuReceive: $roomId") { receiveTask!!.run() }
            sendHelloAsync()
            this.timerTask = HeartBeatTimerTask()
            this.timer.schedule(timerTask, 0, 60000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() {
        synchronized(lock) {
            if (connected) {
                timerTask?.cancel()
                timer.purge()
                timer.cancel()
                receiveTask?.stop()
                outputStream?.close()
                inputStream?.close()
                socket?.close()
                socket = null
            }
        }
    }

    private fun sendMessageAsync(operationCode: DanmakuOperationCode, body: String = ""): Thread {
        val message = DanmakuMessageData().also {
            val messageDataBody = body.toByteArray(Charsets.UTF_8)
            it.operationCode = operationCode.code
            it.packetLength = (16 + messageDataBody.size)
            it.body = messageDataBody
        }
        return sendAsync(PackUtils.pack(message))
    }

    private fun sendHelloAsync(): Thread {
        val messageBody = mapOf(
            "uid" to 0,
            "roomid" to roomId,
            "protover" to 3,
            "platform" to "web",
            "clientver" to "2.6.25",
            "type" to 2,
            "key" to token,
        )
        return sendMessageAsync(DanmakuOperationCode.ENTER_ROOM, gson.toJson(messageBody))
    }

    private fun sendHeartBeatAsync() = sendMessageAsync(DanmakuOperationCode.HEART_BEAT)

    private fun sendAsync(messageBody: ByteArray) = thread {
        synchronized(lock) {
            if (connected) {
                outputStream?.write(messageBody)
                outputStream?.flush()
            }
        }
    }

    private fun dispatchMessage(messageBody: ByteArray) {
        val message = PackUtils.unpack(messageBody)
        for (m in message) {
            println("Message receive: $m")
        }
    }

    inner class HeartBeatTimerTask : TimerTask() {
        override fun run() {
            sendHeartBeatAsync()
        }
    }

    inner class ReceiveMessageThread : Runnable {
        private var stopped = false
        override fun run() {
            while (!stopped && inputStream != null) {
                try {
                    val lengthByte=inputStream!!.readNBytes(4)
                    val length = lengthByte.toInt()
                    if (length == 0) continue
                    if (length == -1) {
                        this.stop()
                        break
                    }
                    val body = inputStream!!.readNBytes(length-4)
                    if (length < 4) continue
                    dispatchMessage(lengthByte+body)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun stop() {
            this.stopped = true
        }

        private fun Int.toByteArray(): ByteArray {
            val b = ByteArray(4)
            b[3] = (this and 0xff).toByte()
            b[2] = (this shr 8 and 0xff).toByte()
            b[1] = (this shr 16 and 0xff).toByte()
            b[0] = (this shr 24 and 0xff).toByte()
            return b
        }
        fun ByteArray.toInt(): Int =
            this[0].toInt().and(0xff).shl(24).or(
                this[1].toInt().and(0xff).shl(16).or(
                    this[2].toInt().and(0xff).shl(8).or(
                        this[3].toInt().and(0xff)
                    )
                )
            )
    }
}