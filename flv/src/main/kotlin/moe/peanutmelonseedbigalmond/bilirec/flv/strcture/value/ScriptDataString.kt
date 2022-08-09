package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream

class ScriptDataString : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.STRING
    lateinit var value: String

    override fun writeTo(stream: OutputStream) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.size > 65535) {
            throw Exception("Cannot write more than 65535 (actual: ${bytes.size}) into ScriptDataString")
        }
        stream.write(
            byteArrayOf(
                type.value.toByte(),
                *(bytes.size.toShort().toByteArray()),
            )
        )
        stream.write(bytes)
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataString"){
            text(value)
        }
    }

    override fun toString(): String {
        return "$type: $value"
    }
}