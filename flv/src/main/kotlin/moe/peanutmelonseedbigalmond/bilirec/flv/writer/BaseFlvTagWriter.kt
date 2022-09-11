package moe.peanutmelonseedbigalmond.bilirec.flv.writer

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import java.io.Closeable

abstract class BaseFlvTagWriter:Closeable {
    abstract fun writeFlvHeader()
    abstract fun writeFlvScriptTag(data: Tag)
    abstract fun writeFlvData(data: Tag)
    abstract fun getFileLength():Long
}