package moe.peanutmelonseedbigalmond.bilirec.flv.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.exception.FLVDataException
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.AudioData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.VideoData
import struct.JavaStruct
import java.io.*
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

/**
 * [flv 结构解析](https://zhuanlan.zhihu.com/p/83346973)
 */
class FlvTagReader(
    private val inputStream: InputStream,
) : Closeable {
    constructor(fileName: String) : this(FileInputStream(fileName))

    private val tagIndex = AtomicLong(0)

    private val readLock = Object()

    @Volatile
    private var closed = false
    private var fileHeader = false

    override fun close() {
        synchronized(readLock) {
            if (closed) return
            closed = true
            inputStream.close()
        }
    }

    suspend fun readNextTagAsync(): Tag? {
        if (closed) return null
        if (!this@FlvTagReader.fileHeader) {
            if (parseFileHeaderAsync(inputStream)) {
                this@FlvTagReader.fileHeader = true
            } else {
                return null
            }
        }
        return try {
            parseTagDataAsync(inputStream)
        } catch (_: IOException) {
            null
        }
    }

    // region 解析数据
    /**
     * 读取 9 字节的 flv 文件头
     */
    private suspend fun parseFileHeaderAsync(stream: InputStream): Boolean {
        val buffer = withContext(Dispatchers.IO) { stream.readNBytes(9) }
        if (buffer.size < 9) return false
        if (String(buffer, 0, 3, Charsets.UTF_8) != "FLV" || buffer[3] != 1.toByte()) {
            throw FLVDataException("Data is not FLV")
        }
        if (buffer[5] != 0.toByte() || buffer[6] != 0.toByte() || buffer[7] != 0.toByte() || buffer[8] != 9.toByte()) {
            throw FLVDataException("Not supported FLV format")
        }
        return true
    }

    /**
     * 读 flv tag
     * 除了 9 字节的公共文件头外，body 部分就是一个个 tag 组成的。每一个 tag 都有 15 字节的 tag 头。字段说明如下：
     * @return 当读取到的不是 flv tag 时，返回 null
     * @throws IOException
     */
    @Throws(IOException::class)
    private suspend fun parseTagDataAsync(inputStream: InputStream): Tag? {
        withContext(Dispatchers.IO) { inputStream.skipNBytes(4) }// 跳过第一个无意义的 Tag
        val data = withContext(Dispatchers.IO) { inputStream.readNBytes(11) }
        if (data.size < 11) { // 读取的字节不足 Tag header 长度，说明最后以一个 Tag 不完整，忽略
            throw EOFException("流已经读取到末尾")
        }
        val tag = Tag()
        JavaStruct.unpack(tag, data, ByteOrder.BIG_ENDIAN)

        if (tag.getTagType() == TagType.UNKNOWN) return null

        when (tag.getTagType()) {
            TagType.AUDIO -> {
                val bArr = withContext(Dispatchers.IO) { inputStream.readNBytes(tag.getDataSize()) }
                val body = AudioData.fromBytes(bArr)
                tag.data = body
            }
            TagType.VIDEO -> {
                val bArray = withContext(Dispatchers.IO) { inputStream.readNBytes(tag.getDataSize()) }
                val body = VideoData.fromBytes(bArray)
                tag.data = body
            }
            TagType.SCRIPT -> {
                val bytes = withContext(Dispatchers.IO) { inputStream.readNBytes(tag.getDataSize()) }
                val body = ScriptData.fromBytes(bytes)
                tag.data = body
            }
            else -> {} // Do nothing
        }

        tag.tagIndex = tagIndex.getAndIncrement()
        return tag
    }
    // endregion
}