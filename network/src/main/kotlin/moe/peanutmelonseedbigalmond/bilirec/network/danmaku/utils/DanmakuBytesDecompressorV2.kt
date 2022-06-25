package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.utils

import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream

object DanmakuBytesDecompressorV2 : BaseDanmakuBytesDecompressor {
    // deflate解压
    override fun decompress(bytes: ByteArray): ByteArray {
        val inputStream = ByteArrayInputStream(bytes)
        val decompressStream = InflaterInputStream(inputStream)
        val decompressed = decompressStream.readAllBytes()
        decompressStream.close()
        inputStream.close()
        return decompressed
    }
}