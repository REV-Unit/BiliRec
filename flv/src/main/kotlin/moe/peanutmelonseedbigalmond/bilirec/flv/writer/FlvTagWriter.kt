package moe.peanutmelonseedbigalmond.bilirec.flv.writer

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile

class FlvTagWriter(private val randomAccessFile: RandomAccessFile) : BaseFlvTagWriter() {

    constructor(fileName: String) : this(RandomAccessFile(fileName, "rw"))

    private val logger = LoggingFactory.getLogger(obj = this)
    private val writeLock = Object()

    @Volatile
    private var closed = false

    override fun writeFlvHeader() {
        synchronized(writeLock) {
            if (closed) return
            randomAccessFile.seek(0)
            randomAccessFile.write(
                byteArrayOf(0x46, 0x4C, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00)
            )
        }
    }

    override fun writeFlvScriptTag(data: Tag) {
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

    override fun writeFlvData(data: Tag) {
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

    override fun getFileLength(): Long = synchronized(writeLock) { this.randomAccessFile.length() }

    fun flush() = synchronized(writeLock) { this.randomAccessFile.fd.sync() }

    override fun close() {
        synchronized(writeLock) {
            if (closed) return
            closed = true
            randomAccessFile.close()
        }
    }
}
