package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.xmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import java.util.*

class ScriptDataStrictArray<T : BaseScriptDataValue>(
    private val arrayList: LinkedList<T> = LinkedList()
) : BaseScriptDataValue(), List<T> by arrayList {
    override val type: ScriptDataType
        get() = ScriptDataType.STRICT_ARRAY

    override fun writeTo(outputStream: OutputStream) {
        outputStream.write(byteArrayOf(type.value.toByte()))
        outputStream.write(size.toByteArray())
        for (item in arrayList) {
            item.writeTo(outputStream)
        }
    }

    override fun dataToXmlElement(): XmlElement {
        return xmlElement("ScriptDataStrictArray"){
            for (v in arrayList){
                v.dataToXmlElement()
            }
        }
    }

    fun add(data: T) = arrayList.add(data)

    fun removeLast() = arrayList.removeLast()

    override fun toString(): String {
        return "$type, count=$size"
    }
}