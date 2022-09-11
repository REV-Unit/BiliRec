package moe.peanutmelonseedbigalmond.bilirec.dsl.xml

import javax.xml.stream.XMLStreamWriter

fun XMLStreamWriter.writeXmlElement(element: XmlElement) {
    element as XmlElement.Element

    if (element.children.isEmpty()) {
        this.writeEmptyElement(element.name)
    } else {
        this.writeStartElement(element.name)
    }

    if (element.attributes.isNotEmpty()) {
        for (attr in element.attributes) {
            this.writeAttribute(attr.name, attr.value)
        }
    }
    if (element.children.isNotEmpty()) {
        for (child in element.children) {
            when (child) {
                is XmlElement.Element -> this.writeXmlElement(child)
                is XmlElement.Text -> this.writeCharacters(child.text)
            }
        }
        this.writeEndElement()
    }
}

inline fun XMLStreamWriter.writeXmlElement(name: String, xmlElementBuilder: XmlElementBuilder.() -> Unit) {
    this.writeXmlElement(XmlElementBuilder(name).apply(xmlElementBuilder).build())
}
