package moe.peanutmelonseedbigalmond.bilirec.flv.strcture

import moe.peanutmelonseedbigalmond.bilirec.flv.Writeable
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.FrameType
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagFlag
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.AudioData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.BaseTagData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.VideoData
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import moe.peanutmelonseedbigalmond.bilirec.flv.toInt
import struct.StructClass
import struct.StructField
import java.io.ByteArrayOutputStream
import java.io.OutputStream

@StructClass
class Tag : Writeable {
    @StructField(order = 0)
    var mTagType: Byte = 0

    @StructField(order = 1)
    var mDataSize = ByteArray(3)

    @StructField(order = 2)
    var mTimestamp = ByteArray(4)

    @StructField(order = 3)
    var mStreamId = ByteArray(3)

    var tagIndex: Long = 0

    lateinit var data: BaseTagData

    val binaryLength: Long
        get() {
            val bos = ByteArrayOutputStream()
            this.writeTo(bos)
            bos.close()
            return bos.size().toLong()
        }

    fun getTagType() = TagType.fromValue(mTagType.toInt())
    fun setTagType(type: TagType) {
        mTagType = type.value.toByte()
    }

    fun getDataSize(): Int = byteArrayOf(0, *mDataSize).toInt()
    fun setDataSize(size: Int) {
        mDataSize = size.toByteArray().takeLast(3).toByteArray()
    }

    fun getStreamId(): Int = byteArrayOf(0, *mStreamId).toInt()
    fun setStreamId(streamId: Int) {
        mStreamId = streamId.toByteArray().takeLast(3).toByteArray()
    }

    fun getTimeStamp(): Int = byteArrayOf(mTimestamp[3], mTimestamp[0], mTimestamp[1], mTimestamp[2]).toInt()
    fun setTimeStamp(timestamp: Int) {
        mTimestamp = timestampToByteArray(timestamp)
    }

    override fun toString(): String {
        return "Tag(tagType=${getTagType()}, dataSize=${getDataSize()}, timestamp=${getTimeStamp()}, streamId=${getStreamId()}, tagIndex=$tagIndex, data=$data)"
    }

    override fun writeTo(outputStream: OutputStream) {
        val tagDataBytes = ByteArrayOutputStream()
        data.writeTo(tagDataBytes)
        val bytes = tagDataBytes.toByteArray()
        tagDataBytes.close()
        val size = bytes.size.toByteArray().takeLast(3).toByteArray()
        outputStream.write(
            byteArrayOf(
                mTagType,
                *size,
                *mTimestamp,
                *mStreamId
            )
        )
        outputStream.write(bytes)
        outputStream.write((bytes.size + 11).toByteArray()) // 每个Tag块最后的长度包含Tag头的长度，所以此处加上Tag头的长度（11字节）
    }

    fun getTagFlag(): TagFlag {
        return when (this.getTagType()) {
            TagType.AUDIO -> (data as AudioData).tagFlag
            TagType.VIDEO -> (data as VideoData).tagFlag
            else -> TagFlag.NONE
        }
    }

    // region Tag 数据类型
    fun isScriptTag(): Boolean =
        this.getTagType() == TagType.SCRIPT

    fun isHeaderTag(): Boolean = getTagFlag() == TagFlag.HEADER

    fun isEndTag(): Boolean = getTagFlag() == TagFlag.END

    fun isDataTag(): Boolean {
        val tagType = getTagType()
        return tagType == TagType.AUDIO || tagType == TagType.VIDEO
    }

    fun isKeyframeData(): Boolean {
        return this.getTagType() == TagType.VIDEO && (this.data as VideoData).frameType == FrameType.KEY_FRAME
    }
    // endregion

    companion object {
        fun timestampToByteArray(timestamp: Int): ByteArray {
            val tsArray = timestamp.toByteArray()
            return byteArrayOf(
                tsArray[1], tsArray[2], tsArray[3], tsArray[0]
            )
        }
    }
}