package moe.peanutmelonseedbigalmond.bilirec.recording.task.impl

import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadErrorEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.events.RecordingThreadExitedEvent
import moe.peanutmelonseedbigalmond.bilirec.recording.task.BaseRecordTask
import org.greenrobot.eventbus.EventBus
import java.io.EOFException
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.concurrent.thread

/**
 *
 * 录制原始数据，不修复
 */
class RawRecordTask(
    private val inputStream: InputStream,
    private val room: Room,
    private val outputFileNamePrefix: String
) : BaseRecordTask(room) {
    @Volatile
    private var writer: FileOutputStream? = null

    @Volatile
    private var worker: Worker? = null
    private val writeLock = Object()

    @Volatile
    private var mClosed = false
    override val closed: Boolean = mClosed
    override fun prepare() {

    }

    private inner class Worker : Runnable {
        @Volatile
        private var cancelled = false
        override fun run() {
            synchronized(writeLock) {
                val buffer = ByteArray(1024 * 1024)
                while (!cancelled) {
                    try {
                        val len = inputStream.read(buffer)
                        if (len == -1) throw EOFException("录制流已到结尾")
                        writer!!.write(buffer, 0, len)
                    } catch (e: Exception) {
                        EventBus.getDefault().post(RecordingThreadErrorEvent(this@RawRecordTask.room, e))
                    }
                }
                EventBus.getDefault().post(RecordingThreadExitedEvent(room))
            }
        }

        fun cancel() {
            cancelled = true
        }
    }

    override fun startAsync(baseFileName:String) {
        if (writer == null) {
            writer = FileOutputStream("${outputFileNamePrefix}_raw.flv")
        }
        thread(name = "RawRecordTask - RoomId:${room.roomConfig.roomId}") {
            worker = Worker()
            worker!!.run()
        }
    }

    override fun stopRecording() {

    }

    override fun close() {
        synchronized(writeLock) {
            if (closed) return
            writer?.close()
            this.writer=null
            worker?.cancel()
            this.worker=null
            inputStream.close()
            mClosed = true
        }
    }
}