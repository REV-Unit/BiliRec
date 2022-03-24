package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import java.io.OutputStream

abstract class BaseScriptDataValue {
    abstract val type: ScriptDataType
    abstract fun writeTo(stream:OutputStream)
}