package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.FrameType
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagFlag
import java.io.OutputStream

class VideoData private constructor() : BaseTagData() {
    override lateinit var binaryData: ByteArray
    var frameType: FrameType = FrameType.NONE
        private set
    var codecId: Int = 0 // 7 = AAC
        private set
    var tagFlag: TagFlag = TagFlag.NONE
        private set

    override fun writeTo(outputStream: OutputStream) {
        val frame = (frameType.value shl 4) or (codecId)
        outputStream.write(frame)
        outputStream.write(binaryData)
    }

    override fun toString(): String {
        return "VideoData(binaryData size=${binaryData.size}, frameType=$frameType, codecId=$codecId, tagFlag=$tagFlag)"
    }

    companion object {
        private val headerMapping = mapOf(0 to TagFlag.HEADER, 2 to TagFlag.END)
        fun fromBytes(byteArray: ByteArray): VideoData {
            val videoData = VideoData()
            val frame = byteArray[0].toInt() and 0xff
            // 取出前四位，为帧类型 Frame Type
            videoData.frameType = FrameType.fromValue((frame and 0xf0) shr 4)
            // 取出后四位，为视频类型
            videoData.codecId = (frame and 0x0f)
            videoData.tagFlag = headerMapping[byteArray[1].toInt() and 0xff] ?: TagFlag.NONE
            // 之后全部为内容
            videoData.binaryData = ByteArray(byteArray.size - 1)
            System.arraycopy(byteArray, 1, videoData.binaryData, 0, byteArray.size - 1)
            return videoData
        }
    }
}