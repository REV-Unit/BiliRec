package moe.peanutmelonseedbigalmond.bilirec.flv

// 大端字节数组 转 大端Int
// 网络字节序列   JVM字节序列
fun ByteArray.toInt(): Int =
    this[0].toInt().and(0xff).shl(24).or(
        this[1].toInt().and(0xff).shl(16).or(
            this[2].toInt().and(0xff).shl(8).or(
                this[3].toInt().and(0xff)
            )
        )
    )

fun ByteArray.toInt16(): Int = ((this[0].toInt() and 0xff) shl 8) or (this[1].toInt() and 0xff)

fun Int.toByteArray(): ByteArray {
    val b = ByteArray(4)
    b[3] = (this and 0xff).toByte()
    b[2] = (this shr 8 and 0xff).toByte()
    b[1] = (this shr 16 and 0xff).toByte()
    b[0] = (this shr 24 and 0xff).toByte()
    return b
}

fun Double.toByteArray(): ByteArray {
    val value = java.lang.Double.doubleToRawLongBits(this)
    val byteRet = ByteArray(8)
    for (i in 0..7) {
        byteRet[7 - i] = (value shr (8 * i) and 0xff).toByte()
    }
    return byteRet
}

fun ByteArray.toDouble(): Double {
    val l = this[0].toLong().and(0xff).shl(56) or
            this[1].toLong().and(0xff).shl(48) or
            this[2].toLong().and(0xff).shl(40) or
            this[3].toLong().and(0xff).shl(32) or
            this[4].toLong().and(0xff).shl(24) or
            this[5].toLong().and(0xff).shl(16) or
            this[6].toLong().and(0xff).shl(8) or
            this[7].toLong().and(0xff)
    return Double.fromBits(l)
}

fun Short.toByteArray(): ByteArray = byteArrayOf(
    (this.toInt() shr 8 and 0xff).toByte(),
    (this.toInt() and 0xff).toByte()
)