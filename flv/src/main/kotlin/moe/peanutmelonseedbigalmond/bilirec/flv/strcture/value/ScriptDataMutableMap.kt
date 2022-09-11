package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream

abstract class ScriptDataMutableMap(private val map: MutableMap<String, BaseScriptDataValue>) :
    MutableMap<String, BaseScriptDataValue> by map, BaseScriptDataValue() {
    protected fun writeKVs(stream: OutputStream) {
        for ((k, v) in this.map) {
            val bytes = k.toByteArray(Charsets.UTF_8)
            if (bytes.size > 65535) {
                throw Exception("Cannot write more than 65535 (actual: ${bytes.size}) into ScriptDataString")
            }
            stream.write(bytes.size.toShort().toByteArray())
            stream.write(bytes)

            v.writeTo(stream)
        }
    }
}