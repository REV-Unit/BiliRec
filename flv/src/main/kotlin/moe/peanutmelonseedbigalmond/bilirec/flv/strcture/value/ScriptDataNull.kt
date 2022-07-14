package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import java.io.OutputStream

class ScriptDataNull:BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.NULL

    override fun writeTo(stream: OutputStream) {
        stream.write(this.type.value)
    }

    override fun toString(): String {
        return "NULL"
    }
}