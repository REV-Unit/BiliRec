package moe.peanutmelonseedbigalmond.bilirec.flv.writer

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import java.io.Closeable

abstract class BaseFlvTagWriter:Closeable {
    abstract fun writeTagGroup(tagGroup:List<Tag>)
}