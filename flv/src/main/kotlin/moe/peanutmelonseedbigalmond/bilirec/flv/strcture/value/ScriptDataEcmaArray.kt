package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import kotlin.properties.Delegates

class ScriptDataEcmaArray(private val map: LinkedHashMap<String, BaseScriptDataValue> = LinkedHashMap()) :
    BaseScriptDataValue(),
    Map<String, BaseScriptDataValue> by map {
    override val type: ScriptDataType
        get() = ScriptDataType.ECMA_ARRAY

    var value by Delegates.notNull<BaseScriptDataValue>()

    fun toScriptDataObject(): ScriptDataObject = ScriptDataObject(this.map)

    override fun writeTo(stream: OutputStream) {
        stream.write(byteArrayOf(type.value.toByte()))
        stream.write(this.size.toByteArray())
        for ((k, v) in this) {
            val bytes = k.toByteArray(Charsets.UTF_8)
            if (bytes.size > 65535) {
                throw Exception("Cannot write more than 65535 (actual: ${bytes.size}) into ScriptDataString")
            }
            stream.write(bytes.size.toShort().toByteArray())
            stream.write(bytes)

            v.writeTo(stream)
        }
        stream.write(byteArrayOf(0, 0, 9))
    }

    operator fun set(key: String, value: BaseScriptDataValue) {
        this.map[key] = value
    }

    operator fun set(key: ScriptDataString, value: BaseScriptDataValue) {
        this.map[key.value] = value
    }

    override fun toString(): String {
        return "$type, Count=${size}"
    }
}