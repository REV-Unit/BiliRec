package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.utils

internal interface BaseDanmakuBytesDecompressor {
    fun decompress(bytes:ByteArray):ByteArray
}