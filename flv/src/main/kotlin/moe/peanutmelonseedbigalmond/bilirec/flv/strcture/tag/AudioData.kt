package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.FrameType
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagFlag
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.ByteArrayInputStream
import java.io.OutputStream
import kotlin.experimental.and

class AudioData private constructor() : BaseTagData() {
    override lateinit var binaryData: ByteArray
    var tagFlag: TagFlag = TagFlag.NONE
        private set
    var audioFormat: Int = 0 // 音频格式，0=Linear PCM, 1=ADPCM, 2=MP3, 10=AAC
        private set
    var bitrate: Int = 0 // 音频比特率，0=5.5KHz, 1=11KHz, 2=22KHz, 3=44KHz. 对于AAC该值总为3
        private set
    var bit: Int = 0 // 采样大小，0=8比特，1=16比特
        private set
    var stereo: Int = 0 // 声道数，0=单声道，1=立体声
        private set

    override fun writeTo(outputStream: OutputStream) {
        outputStream.write(binaryData)
    }

    override fun toString(): String {
        return "AudioData(binaryData size=${binaryData.size}, " +
                "tagFlag=$tagFlag, " +
                "audioFormat=$audioFormat, " +
                "bitrate=$bitrate, " +
                "bit=$bit, " +
                "stereo=$stereo)"
    }

    companion object {
        fun fromBytes(byteArray: ByteArray): AudioData {
            val audioData = AudioData()
            val firstByte = byteArray[0]
            assignInfo(audioData, firstByte)
            if (audioData.audioFormat == 10) { // 音频格式AAC
                if ((byteArray[1].toInt() and 0xff) == 0) {
                    audioData.tagFlag = TagFlag.HEADER
                }
            }
            audioData.binaryData = byteArray
            return audioData
        }

        private fun assignInfo(data: AudioData, firstByte: Byte) {
            val byte = firstByte.toInt() and 0xff
            data.audioFormat = (byte and 0b1111_0000) shr 4
            data.bitrate = (byte and 0b00_00_11_00) shr 2
            data.bit = (byte and 0b00_00_00_10) shr 1
            data.stereo = (byte and 0b0000_0001)
        }
    }
}