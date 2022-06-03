package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import kotlin.math.abs

class KeyframesObject(map: LinkedHashMap<String, BaseScriptDataValue> = LinkedHashMap()) :
    ScriptDataObject(map) {
    init {
        val timesArray = ScriptDataStrictArray()
        val filePosArray = ScriptDataStrictArray()

        map[KEY_TIMES] = timesArray
        map[KEY_FILE_POSITIONS] = filePosArray
//        map[KEY_SPACER]=ScriptDataStrictArray(LinkedList(MutableList(MAX_KEYFRAME_COUNT*2){ScriptDataNumber.assignValueNan()}))
    }

    override fun writeTo(stream: OutputStream) {
        val timesArray = this[KEY_TIMES]!! as ScriptDataStrictArray
        val filePositionsArray = this[KEY_FILE_POSITIONS]!! as ScriptDataStrictArray

        stream.write(byteArrayOf(this.type.value.toByte()))

        writeKey(stream, KEY_TIMES)
        writeStrictArray(stream, timesArray)

        writeKey(stream, KEY_FILE_POSITIONS)
        writeStrictArray(stream, filePositionsArray)

        val count = (MAX_KEYFRAME_COUNT - timesArray.size) * 2
        writeKey(stream, KEY_SPACER)
        stream.write(byteArrayOf(ScriptDataType.STRICT_ARRAY.value.toByte()))
        stream.write(count.toByteArray())
        repeat(count) {
            ScriptDataNumber.assignValueNan().writeTo(stream)
        }

        stream.write(byteArrayOf(0, 0, 9))
    }

    private fun writeKey(outputStream: OutputStream, key: String) {
        val bytes = key.toByteArray(Charsets.UTF_8)
        if (bytes.size > 65535) {
            throw Exception("Cannot write more than 65535 (actual: ${bytes.size}) into ScriptDataString")
        }
        outputStream.write(bytes.size.toShort().toByteArray())
        outputStream.write(bytes)
    }

    private fun writeStrictArray(outputStream: OutputStream, array: ScriptDataStrictArray) {
        array.writeTo(outputStream)
    }

    fun addKeyframe(keyframeTimeMillisecond: Double, keyframePosition: Long) {
        if ((this[KEY_TIMES]!! as ScriptDataStrictArray).size >= MAX_KEYFRAME_COUNT) return
        val previousTime = ((this[KEY_TIMES]!! as ScriptDataStrictArray).lastOrNull() as ScriptDataNumber?)?.value
            ?: Double.NEGATIVE_INFINITY
        if (abs(keyframeTimeMillisecond - previousTime) < MAX_INTERVAL) {
            return
        } else {
            (this[KEY_TIMES]!! as ScriptDataStrictArray).add(ScriptDataNumber.assign(keyframeTimeMillisecond / 1000.0))
            (this[KEY_FILE_POSITIONS]!! as ScriptDataStrictArray).add(ScriptDataNumber.assign(keyframePosition.toDouble()))
        }
//        repeat(2){
//            (this[KEY_SPACER]!!as ScriptDataStrictArray).removeLast()
//        }
    }

    companion object {
        const val KEY_TIMES = "times"
        const val KEY_FILE_POSITIONS = "filepositions"
        const val KEY_SPACER = "spacer"

        /**
         * 关键帧的配置
         * 按照最多 9,000 关键帧，每两个关键帧间隔 2,000ms 计算
         * 最多可保证长度小于 9,000*2,000/1,000=18,000s=5h
         * 都能够有关键帧索引
         */

        const val MAX_KEYFRAME_COUNT = 9000 // 最多保存多少个关键帧索引
        const val MAX_INTERVAL = 2000 // 每两个关键帧索引之间的最小长度
    }
}