package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import kotlin.math.abs

class KeyframesObject : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.OBJECT
    private val timesArray = ScriptDataStrictArray<ScriptDataNumber>()
    private val filePosArray = ScriptDataStrictArray<ScriptDataNumber>()

    override fun writeTo(outputStream: OutputStream) {
        outputStream.write(byteArrayOf(this.type.value.toByte()))

        writeKey(outputStream, KEY_TIMES)
        writeStrictArray(outputStream, timesArray)

        writeKey(outputStream, KEY_FILE_POSITIONS)
        writeStrictArray(outputStream, filePosArray)

        val count = (MAX_KEYFRAME_COUNT - timesArray.size) * 2
        writeKey(outputStream, KEY_SPACER)
        outputStream.write(byteArrayOf(ScriptDataType.STRICT_ARRAY.value.toByte()))
        outputStream.write(count.toByteArray())
        repeat(count) {
            ScriptDataNumber.assignValueNan().writeTo(outputStream)
        }

        outputStream.write(byteArrayOf(0, 0, 9))
    }

    private fun writeKey(outputStream: OutputStream, key: String) {
        val bytes = key.toByteArray(Charsets.UTF_8)
        if (bytes.size > 65535) {
            throw Exception("Cannot write more than 65535 (actual: ${bytes.size}) into ScriptDataString")
        }
        outputStream.write(bytes.size.toShort().toByteArray())
        outputStream.write(bytes)
    }

    private fun writeStrictArray(outputStream: OutputStream, array: ScriptDataStrictArray<*>) {
        array.writeTo(outputStream)
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("Keyframes") {
            attribute("count", timesArray.toString())
            for (i in timesArray.indices) {
                val t = timesArray[i]
                val pos = filePosArray[i]
                xmlElement("Item") {
                    attribute("Time", t.value.toString())
                    attribute("Position", pos.value.toString())
                }
            }
        }
    }

    fun addKeyframe(keyframeTimeMillisecond: Double, keyframePosition: Long) {
        if (timesArray.size >= MAX_KEYFRAME_COUNT) return
        val previousTime = timesArray.lastOrNull()?.value
            ?: Double.NEGATIVE_INFINITY
        if (abs(keyframeTimeMillisecond - previousTime) < MAX_INTERVAL) {
            return
        } else {
            timesArray.add(ScriptDataNumber.assign(keyframeTimeMillisecond / 1000.0))
            filePosArray.add(ScriptDataNumber.assign(keyframePosition.toDouble()))
        }
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