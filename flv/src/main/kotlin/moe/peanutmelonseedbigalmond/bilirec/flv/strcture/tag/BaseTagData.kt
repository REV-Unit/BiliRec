package moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag

import moe.peanutmelonseedbigalmond.bilirec.flv.Writeable

abstract class BaseTagData : Writeable {
    abstract var binaryData: ByteArray
    override fun toString(): String {
        return "BaseTagData(binaryData size=${binaryData.size}, type=${this.javaClass.name})"
    }
}