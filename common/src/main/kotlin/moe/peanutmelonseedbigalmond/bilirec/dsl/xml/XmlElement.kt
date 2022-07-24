package moe.peanutmelonseedbigalmond.bilirec.dsl.xml

sealed class XmlElement(val name: String) {

    data class Element(val tagName: String) : XmlElement(tagName) {
        var text: String? = null
        var attributes: MutableList<XmlAttribute> = mutableListOf()
        var children: MutableList<XmlElement> = mutableListOf()
    }

    data class Text(val text: String) : XmlElement(text)

}