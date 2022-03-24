package moe.peanutmelonseedbigalmond.bilirec.flv

internal object BinaryConvertUtils {
    private val lookup32 = createLookup32()
    private fun createLookup32(): IntArray {
        val result = IntArray(256) {
            val s = String.format("%02X", it)
            return@IntArray s[0].code + (s[1].code.shl(16))
        }
        return result
    }

    internal fun byteArrayToHexString(bytes: ByteArray): String = byteArrayToHexString(bytes, 0, bytes.size)

    internal fun byteArrayToHexString(bytes: ByteArray, start: Int, length: Int): String {
        var temp = lookup32
        val result = CharArray(length * 2)
        for (i in start until length) {
            val value = lookup32[bytes[i].toInt()]
            result[2 * i] = value.toChar()
            result[2 * i + 1] = value.shr(16).toChar()
        }
        return String(result)
    }

    internal fun hexStringToByteArray(str: String): ByteArray {
        val array = ByteArray(str.length / 2)
        for (i in str.indices step 2) {
            array[i / 2] = str.substring(i, 2).toByte(16)
        }
        return array
    }
}