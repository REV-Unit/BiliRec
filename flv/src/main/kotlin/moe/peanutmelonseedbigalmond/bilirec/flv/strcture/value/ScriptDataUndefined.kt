package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import java.io.OutputStream

class ScriptDataUndefined : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.UNDEFINED

    override fun writeTo(stream: OutputStream) {
        stream.write(type.value)
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataUndefined"){
            text("Undefined")
        }
    }

    override fun toString(): String {
        return "UNDEFINED"
    }
}