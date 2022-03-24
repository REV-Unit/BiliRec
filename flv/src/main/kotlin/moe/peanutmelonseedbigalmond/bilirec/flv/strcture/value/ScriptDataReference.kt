package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import kotlin.properties.Delegates

class ScriptDataReference : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.REFERENCE

    var value by Delegates.notNull<Int>()

    override fun writeTo(stream: OutputStream) {
        val data = byteArrayOf(
            type.value.toByte(),
            *value.toShort().toByteArray()
        )
        stream.write(data)
    }

    override fun toString(): String {
        return "$type, $value"
    }
}