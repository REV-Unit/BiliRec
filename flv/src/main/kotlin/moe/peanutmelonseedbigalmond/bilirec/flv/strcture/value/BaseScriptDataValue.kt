package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.Writeable
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType

abstract class BaseScriptDataValue : Writeable {
    abstract val type: ScriptDataType
}