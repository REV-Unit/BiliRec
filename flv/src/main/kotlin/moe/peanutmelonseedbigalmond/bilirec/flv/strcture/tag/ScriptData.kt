package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.FrameType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.*
import moe.peanutmelonseedbigalmond.bilirec.flv.toDouble
import moe.peanutmelonseedbigalmond.bilirec.flv.toInt
import moe.peanutmelonseedbigalmond.bilirec.flv.toInt16
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.IllegalArgumentException
import kotlin.collections.ArrayList

class ScriptData constructor(private val list: LinkedList<BaseScriptDataValue> =LinkedList()) : BaseTagData() {
    override lateinit var binaryData: ByteArray

    init {
        val bos=ByteArrayOutputStream()
        for (value in list) {
            value.writeTo(bos)
        }
        this.binaryData=bos.toByteArray()
    }

    val binaryLength: Long
        get() {
            val bos = ByteArrayOutputStream()
            this.writeTo(bos)
            bos.close()
            return bos.size().toLong()
        }

    val size: Int
        get() = this.list.size

    operator fun get(index: Int) = this.list[index]

    operator fun set(index: Int, data: BaseScriptDataValue) {
        this.list[index] = data
    }

    override fun writeTo(outputStream: OutputStream) {
        for (l in list) {
            l.writeTo(outputStream)
        }
    }

    override fun toString(): String {
        return "ScriptData(list size=${list.size}, binaryData size=${binaryData.size})"
    }

    companion object {
        fun fromBytes(byteArray: ByteArray): ScriptData {
            val list = LinkedList<BaseScriptDataValue>()
            val bas = ByteArrayInputStream(byteArray)
            while (bas.available() > 0) {
                list.add(parseValue(bas))
            }
            bas.close()
            return ScriptData(list)
        }

        private fun parseValue(inputStream: InputStream): BaseScriptDataValue {
            val r = inputStream.read()
            val type = ScriptDataType.fromValue(r)
            return when (type) {
                ScriptDataType.NUMBER -> {
                    ScriptDataNumber().also {
                        it.value = inputStream.readDouble()
                    }
                }
                ScriptDataType.BOOLEAN -> ScriptDataBoolean().also {
                    it.value = inputStream.readBoolean()
                }
                ScriptDataType.STRING -> readScriptDataString(inputStream, false)
                    ?: ScriptDataString().also { it.value = "" }
                ScriptDataType.OBJECT -> {
                    val result = ScriptDataObject()
                    while (true) {
                        val propertyName = readScriptDataString(inputStream, true) ?: break
                        val propertyData = parseValue(inputStream)
                        result[propertyName] = propertyData
                    }
                    result
                }
                ScriptDataType.MOVE_CLIP -> throw Exception("MovieClip is not support")
                ScriptDataType.NULL -> throw IllegalArgumentException("Script data is null")
                ScriptDataType.UNDEFINED -> throw IllegalArgumentException("Script data undefined")
                ScriptDataType.REFERENCE -> ScriptDataReference().also { it.value = inputStream.readInt16() }
                ScriptDataType.ECMA_ARRAY -> {
                    val arraySize = inputStream.readInt32()
                    val result = ScriptDataEcmaArray()
                    for (i in 0 until arraySize) {
                        val propertyName = readScriptDataString(inputStream, true) ?: break
                        val propertyData = parseValue(inputStream)
                        result[propertyName] = propertyData
                    }
                    val endMarker = ScriptDataType.fromValue(byteArrayOf(0, *inputStream.readNBytes(3)).toInt())
                    assert(endMarker == ScriptDataType.OBJECT_END_MARKER)
                    result
                }
                ScriptDataType.OBJECT_END_MARKER -> throw Exception("Read ObjectEndMarker")
                ScriptDataType.STRICT_ARRAY -> {
                    val length = inputStream.readInt32()
                    val res = ScriptDataStrictArray()
                    for (i in 0 until length) {
                        val v = parseValue(inputStream)
                        res.add(v)
                    }
                    res
                }
                ScriptDataType.DATE -> {
                    val dateTime = inputStream.readDouble()
                    val offset = inputStream.readInt16()
                    return ScriptDataDate(dateTime, offset)
                }
                ScriptDataType.LONG_STRING -> {
                    val length = inputStream.readInt32()
                    if (length > Int.MAX_VALUE) {
                        throw Exception("LongString lager than ${Int.MAX_VALUE} (actual ${length}) is not supported")
                    }
                    val bytes = inputStream.readNBytes(length)
                    val str = bytes.toString(Charsets.UTF_8).trim(0.toChar())
                    ScriptDataLongString().also { it.value = str }
                }
                else -> throw Exception("Unknown ScriptDataValueType")
            }
        }

        private fun readScriptDataString(inputStream: InputStream, exceptObjectEndMarker: Boolean): ScriptDataString? {
            val length = inputStream.readInt16()
            if (length == 0) {
                if (exceptObjectEndMarker && inputStream.read() != 9) {
                    throw Exception("ObjectEndMarker not matched")
                }
                return null
            }
            return ScriptDataString().also {
                it.value = inputStream.readNBytes(length).toString(Charsets.UTF_8).trim(0.toChar())
            }
        }

        private fun InputStream.readDouble(): Double {
            return this.readNBytes(8).toDouble()
        }

        private fun InputStream.readInt16(): Int {
            val array = this.readNBytes(2)
            return array.toInt16()
        }

        private fun InputStream.readInt32(): Int {
            val byteArray = this.readNBytes(4)
            return byteArray.toInt()
        }

        private fun InputStream.readBoolean(): Boolean = this.read() != 0
    }
}