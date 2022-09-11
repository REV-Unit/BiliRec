package moe.peanutmelonseedbigalmond.bilirec.flv.writer

import com.sun.xml.txw2.output.IndentingXMLStreamWriter
import moe.peanutmelonseedbigalmond.bilirec.dsl.xml.writeXmlElement
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
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

    @Synchronized
    override fun writeFlvHeader() {

    }

    override fun writeFlvScriptTag(data: Tag) {
        if (closed) return
        if (data.getTagType() != TagType.SCRIPT) return
        val tagData = data.data as ScriptData
        xmlWriter.writeXmlElement(tagData.dataToXmlElement())
    }

    override fun writeFlvData(data: Tag) {
        xmlWriter.writeXmlElement(data.dataToXmlElement())
    }

    override fun getFileLength(): Long {
        return 0
    }

    override fun close() {
        xmlWriter.writeEndElement()
        xmlWriter.close()
        fileOutputStream.close()
    }
}