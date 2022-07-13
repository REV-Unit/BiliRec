package moe.peanutmelonseedbigalmond.bilirec.network.danmaku

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuMessageData
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data.DanmakuModel
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuOperationCode
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.utils.DanmakuBytesDecompressorV2
import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.utils.DanmakuBytesDecompressorV3
import struct.JavaStruct
import java.io.ByteArrayInputStream
import java.nio.ByteOrder

internal object PackUtils {
    fun pack(data: DanmakuMessageData): ByteArray {
        return JavaStruct.pack(data, ByteOrder.BIG_ENDIAN)
    }

    fun unpack(byteArray: ByteArray): ArrayList<DanmakuModel> {
        val data = DanmakuMessageData()
        JavaStruct.unpack(data, byteArray, ByteOrder.BIG_ENDIAN)
        val body = byteArray.copyOfRange(16, byteArray.size)
        val operation = DanmakuOperationCode.parse(data.operationCode)
        val version = data.version.toInt()
        if (operation != DanmakuOperationCode.COMMAND) return arrayListOf()
        if (version != 2 && version != 3) return arrayListOf()
        val decompressor = when (version) {
            2 -> DanmakuBytesDecompressorV2
            3 -> DanmakuBytesDecompressorV3
            else -> throw Exception()// unreachable
        }
        val decompressed = decompressor.decompress(body)
        return parseDecompressedDanmakuData(decompressed)
    }

    private fun parseDecompressedDanmakuData(byteArray: ByteArray): ArrayList<DanmakuModel> {
        val res = arrayListOf<DanmakuModel>()
        val stream = ByteArrayInputStream(byteArray)
        while (stream.available() > 0) {
            val data = DanmakuMessageData()
            var bytes = stream.readNBytes(16)
            JavaStruct.unpack(data, bytes, ByteOrder.BIG_ENDIAN)
            val bodyLength = data.packetLength - data.headerLength
            bytes = stream.readNBytes(bodyLength)
            val body = bytes.toString(Charsets.UTF_8)
            res.add(DanmakuModel.fromJson(body))
        }
        stream.close()
        return res
    }
}