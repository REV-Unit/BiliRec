package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagwriter

import com.sun.xml.txw2.output.IndentingXMLStreamWriter
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.writeXmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.writer.BaseFlvTagWriter
import java.io.FileOutputStream
import javax.xml.stream.XMLOutputFactory

class DiagnosisFlvTagWriter(fileNameWithoutExtension: String) : BaseFlvTagWriter() {
    private val fileOutputStream = FileOutputStream("${fileNameWithoutExtension}_data.xml")
    private var xmlWriter = IndentingXMLStreamWriter(
        XMLOutputFactory.newInstance().createXMLStreamWriter(fileOutputStream, Charsets.UTF_8.name())
    )
    private var closed = false

    init {
        xmlWriter.writeStartDocument()
        xmlWriter.writeStartElement("Flv")
    }

    override fun writeTagGroup(tagGroup: List<Tag>) {
        xmlWriter.writeXmlElement("TagGroup") {
            tagGroup.forEach(this@DiagnosisFlvTagWriter::writeFlvData)
        }
    }

    private fun writeFlvData(data: Tag) {
        xmlWriter.writeXmlElement(data.dataToXmlElement())
    }

    override fun close() {
        if (closed) return
        closed = true
        xmlWriter.writeEndElement()
        xmlWriter.close()
        fileOutputStream.close()
    }
}