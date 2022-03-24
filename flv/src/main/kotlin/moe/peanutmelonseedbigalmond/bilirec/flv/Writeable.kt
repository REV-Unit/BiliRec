package moe.peanutmelonseedbigalmond.bilirec.flv

import java.io.OutputStream

interface Writeable {
    fun writeTo(outputStream: OutputStream)
}