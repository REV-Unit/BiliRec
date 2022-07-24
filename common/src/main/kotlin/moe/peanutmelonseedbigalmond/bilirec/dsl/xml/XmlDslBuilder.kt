package moe.peanutmelonseedbigalmond.bilirec.dsl.xml

@XmlDsl
class XmlElementBuilder(val name: String) {
    private val attributes = mutableListOf<XmlAttribute>()
    val children = mutableListOf<XmlElement>()
    private var text: String? = null

    fun attribute(name: String, value: String) {
        attributes.add(XmlAttribute(name, value))
    }

    inline fun child(name: String, builder: XmlElementBuilder.() -> Unit) {
        children.add(XmlElementBuilder(name).apply(builder).build())
    }

    fun text(text: String) {
        children.add(XmlElement.Text(text))
    }

    fun build(): XmlElement.Element {
        val element = XmlElement.Element(name)
        element.text = text
        element.attributes = attributes
        element.children = children
        return element
    }
}
