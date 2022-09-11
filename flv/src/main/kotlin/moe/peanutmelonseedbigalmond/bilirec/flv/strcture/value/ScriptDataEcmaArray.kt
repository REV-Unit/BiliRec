package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import kotlin.properties.Delegates

class ScriptDataEcmaArray(private val map: LinkedHashMap<String, BaseScriptDataValue> = LinkedHashMap()) :
    ScriptDataMutableMap(map) {
    override val type: ScriptDataType
        get() = ScriptDataType.ECMA_ARRAY

    var value by Delegates.notNull<BaseScriptDataValue>()

    fun toScriptDataObject(): ScriptDataObject = ScriptDataObject(this.map)

    override fun writeTo(outputStream: OutputStream) {
        outputStream.write(byteArrayOf(type.value.toByte()))
        outputStream.write(this.size.toByteArray())
        writeKVs(outputStream)
        outputStream.write(byteArrayOf(0, 0, 9))
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataEcmaArray") {
            attribute("count", this@ScriptDataEcmaArray.size.toString())
            for ((k, v) in this@ScriptDataEcmaArray) {
                xmlElement("Item") {
                    xmlElement("Key") { text(k) }
                    xmlElement("Value") {
                        child(v.dataToXmlElement())
                    }
                }
            }
        }
    }

    operator fun set(key: String, value: BaseScriptDataValue) {
        map[key] = value
    }

    override operator fun get(key: String) = map[key]

    override fun toString(): String {
        return "$type[$map, Count=${size}]"
    }
}