package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.utils

import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream

object DanmakuBytesDecompressorV3:BaseDanmakuBytesDecompressor {
    // brotli解压
    override fun decompress(bytes: ByteArray):ByteArray {
        val inputStream=ByteArrayInputStream(bytes)
        val decompressStream=BrotliInputStream(inputStream)
        val decompressed=decompressStream.readAllBytes()
        decompressStream.close()
        inputStream.close()
        return decompressed
    }
}