package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import java.io.OutputStream

class ScriptDataUndefined : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.UNDEFINED

    override fun writeTo(stream: OutputStream) {
        stream.write(type.value)
    }

    override fun toString(): String {
        return "UNDEFINED"
    }
}