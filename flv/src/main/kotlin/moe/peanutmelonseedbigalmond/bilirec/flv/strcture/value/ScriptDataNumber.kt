package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import kotlin.properties.Delegates

class ScriptDataNumber : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.NUMBER
    var value by Delegates.notNull<Double>()

    override fun writeTo(stream: OutputStream) {
        val bytes = value.toByteArray()
        val data = byteArrayOf(
            type.value.toByte(),
            *bytes
        )
        stream.write(data)
    }

    override fun toString(): String {
        return "$type: $value"
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataNumber") {
            text(value.toString())
        }
    }

    companion object {
        fun assign(value: Double = 0.0) = ScriptDataNumber().also { it.value = value }
        fun assignValueNan(): ScriptDataNumber = assign(Double.NaN)
    }
}