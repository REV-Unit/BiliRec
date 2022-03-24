package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.ScriptDataType
import moe.peanutmelonseedbigalmond.bilirec.flv.toByteArray
import java.io.OutputStream
import java.time.*

class ScriptDataDate() : BaseScriptDataValue() {
    override val type: ScriptDataType
        get() = ScriptDataType.DATE

    lateinit var value: OffsetDateTime

    constructor(value: OffsetDateTime) : this() {
        this.value = value
    }

    constructor(timestamp: Double, offsetMinutesBasedUTC: Int) : this() {
        this.value =
            Instant.ofEpochMilli(timestamp.toLong()).atOffset(ZoneOffset.ofTotalSeconds(offsetMinutesBasedUTC * 60))
    }

    override fun writeTo(stream: OutputStream) {
        val offsetMinutesBasedUTC=value.offset.totalSeconds/60
        val timestamp=value.toInstant().toEpochMilli().toDouble()
        stream.write(byteArrayOf(type.value.toByte()))
        stream.write(timestamp.toByteArray())
        stream.write(offsetMinutesBasedUTC.toShort().toByteArray())
    }

    override fun toString(): String {
        return "$type: $value"
    }
}