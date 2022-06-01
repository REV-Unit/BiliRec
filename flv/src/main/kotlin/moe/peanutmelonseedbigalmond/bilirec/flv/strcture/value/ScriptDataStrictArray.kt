package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import java.util.*

class ScriptDataStrictArray(
    private val arrayList: LinkedList<BaseScriptDataValue> = LinkedList()
) : BaseScriptDataValue(), List<BaseScriptDataValue> by arrayList {
    override val type: ScriptDataType
        get() = ScriptDataType.STRICT_ARRAY

    override fun writeTo(stream: OutputStream) {
        stream.write(byteArrayOf(type.value.toByte()))
        stream.write(size.toByteArray())
        for (item in arrayList) {
            item.writeTo(stream)
        }
    }

    fun add(data: BaseScriptDataValue) = arrayList.add(data)

    fun removeLast()=arrayList.removeLast()

    override fun toString(): String {
        return "$type, count=$size"
    }
}