package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import java.io.OutputStream
import kotlin.properties.Delegates

class ScriptDataBoolean : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.BOOLEAN
    var value by Delegates.notNull<Boolean>()

    override fun writeTo(stream: OutputStream) {
        val data = byteArrayOf(
            type.value.toByte(),
            (if (value) 1 else 0).toByte()
        )
        stream.write(data)
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataBoolean") {
            text(value.toString())
        }
    }

    override fun toString(): String {
        return "$type: $value"
    }
}