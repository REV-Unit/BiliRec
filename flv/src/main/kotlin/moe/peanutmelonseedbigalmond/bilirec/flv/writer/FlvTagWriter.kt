package moe.peanutmelonseedbigalmond.bilirec.flv.writer

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile

class FlvTagWriter(private val randomAccessFile: RandomAccessFile) : AutoCloseable {

    constructor(fileName: String) : this(RandomAccessFile(fileName, "rw"))

    private val logger = LoggingFactory.getLogger(obj = this)
    private val writeLock = Object()

    @Volatile
    private var closed = false

    fun writeFlvHeader() {
        synchronized(writeLock) {
            if (closed) return
            randomAccessFile.seek(0)
            randomAccessFile.write(
                byteArrayOf(0x46, 0x4C, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00)
            )
        }
    }

    fun writeFlvScriptTag(data: Tag) {
        synchronized(writeLock) {
            if (closed) return
            if (data.getTagType() != TagType.SCRIPT && data.data !is ScriptData) {
                logger.warn("Tag 不是 SCRIPT，忽略（类型： ${data.getTagType()}，数据类型： ${data.data.javaClass.simpleName}）")
                return
            }
            this.randomAccessFile.seek(3 + 1 + 1 + 4 + 4)
            val bos = ByteArrayOutputStream()
            data.writeTo(bos)
            this.randomAccessFile.write(bos.toByteArray())
            bos.close()
        }
    }

    fun writeFlvData(data: Tag) {
        synchronized(writeLock) {
            if (closed) return
            if (data.getTagType() != TagType.SCRIPT) {
                this.randomAccessFile.seek(this.randomAccessFile.length())
                val bos = ByteArrayOutputStream()
                data.writeTo(bos)
                this.randomAccessFile.write(bos.toByteArray())
                bos.close()
            } else {
                logger.warn("写入直播数据中途遇到意外的Script数据块，忽略")
            }
        }
    }

    fun getFilePointer(): Long = synchronized(writeLock) { this.randomAccessFile.filePointer }

    fun getFileLength(): Long = synchronized(writeLock) { this.randomAccessFile.length() }

    fun flush()= synchronized(writeLock){this.randomAccessFile.fd.sync()}

    override fun close() {
        synchronized(writeLock){
            if (closed) return
            closed = true
            randomAccessFile.close()
        }
    }
}

/**
 * TODO: Write方法可能会抛出异常
 * 例如当磁盘空间不足的时候
 * 下面是异常堆栈
 * java.io.IOException: 磁盘空间不足。
 *     at java.base/java.io.RandomAccessFile.writeBytes(Native Method)
 *     at java.base/java.io.RandomAccessFile.write(RandomAccessFile.java:545)
 *     at moe.peanutmelonseedbigalmond.bilirec.flv.writer.FlvTagWriter.writeFlvData(FlvTagWriter.kt:54)
 *     at moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext.writeVideoChunk(LiveStreamRepairContext.kt:168)
 *     at moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext.writeTag(LiveStreamRepairContext.kt:111)
 *     at moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext.access$writeTag(LiveStreamRepairContext.kt:27)
 *     at moe.peanutmelonseedbigalmond.bilirec.recording.repair.context.LiveStreamRepairContext$createFlvWriteJob$1.invokeSuspend(LiveStreamRepairContext.kt:88)
 *     at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
 *     at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
 *     at kotlinx.coroutines.internal.LimitedDispatcher.run(LimitedDispatcher.kt:42)
 *     at kotlinx.coroutines.scheduling.TaskImpl.run(Tasks.kt:95)
 *     at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:570)
 *     at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:749)
 *     at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:677)
 *     at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:664)
 */
