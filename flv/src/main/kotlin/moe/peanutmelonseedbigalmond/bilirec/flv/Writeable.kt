package moe.peanutmelonseedbigalmond.bilirec.flv

import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.XmlElement
import java.io.OutputStream

interface Writeable {
    fun writeTo(outputStream: OutputStream)

    fun dataToXmlElement(): XmlElement
}