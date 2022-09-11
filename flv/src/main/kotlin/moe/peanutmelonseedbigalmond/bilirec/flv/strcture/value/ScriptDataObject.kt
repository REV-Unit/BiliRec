package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import java.io.OutputStream

class ScriptDataObject(private val map: LinkedHashMap<String, BaseScriptDataValue> = LinkedHashMap()) :
    ScriptDataMutableMap(map) {
    override val type: ScriptDataType
        get() = ScriptDataType.OBJECT

    fun toScriptDataEcmaArray(): ScriptDataEcmaArray = ScriptDataEcmaArray(this.map)

    override fun writeTo(outputStream: OutputStream) {
        outputStream.write(byteArrayOf(type.value.toByte()))
        writeKVs(outputStream)
        outputStream.write(byteArrayOf(0, 0, 9))
    }

    operator fun set(key: String, value: BaseScriptDataValue) {
        this.map[key] = value
    }

    operator fun set(key: ScriptDataString, value: BaseScriptDataValue) {
        this.map[key.value] = value
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataObject") {
            attribute("count", this@ScriptDataObject.size.toString())
            for ((k, v) in this@ScriptDataObject) {
                child("Item") {
                    attribute("key", k)
                    child(v.dataToXmlElement())
                }
            }
        }
    }

    override fun toString(): String {
        return "$type[$map, Count=${size}]"
    }

}