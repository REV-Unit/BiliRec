package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream

class ScriptDataLongString : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.LONG_STRING

    var value: String = ""

    override fun writeTo(stream: OutputStream) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val buffer = bytes.size.toByteArray()
        stream.write(
            byteArrayOf(
                this.value.toByte(),
                *buffer
            )
        )
        stream.write(bytes)
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataLongString") {
            text(value)
        }
    }

    override fun toString(): String {
        return "$type, $value"
    }
}