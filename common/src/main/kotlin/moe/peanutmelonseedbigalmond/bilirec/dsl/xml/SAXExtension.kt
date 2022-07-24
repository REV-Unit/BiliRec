package moe.peanutmelonseedbigalmond.bilirec.dsl.xml

import javax.xml.stream.XMLStreamWriter

fun XMLStreamWriter.writeXmlElement(element: XmlElement) {
    this.writeStartElement(element.name)
    element as XmlElement.Element
    for (attr in element.attributes) {
        this.writeAttribute(attr.name, attr.value)
    }

    for (child in element.children) {
        when(child){
            is XmlElement.Element-> this.writeXmlElement(child)
            is XmlElement.Text->this.writeCharacters(child.text)
        }
    }
    this.writeEndElement()
}

inline fun XMLStreamWriter.writeXmlElement(name:String,xmlElementBuilder: XmlElementBuilder.() -> Unit) {
    this.writeXmlElement(XmlElementBuilder(name).apply(xmlElementBuilder).build())
}
