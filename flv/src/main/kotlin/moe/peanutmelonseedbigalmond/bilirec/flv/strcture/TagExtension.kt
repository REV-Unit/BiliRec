package moe.peanutmelonseedbigalmond.bilirec.flv.strcture

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.FrameType
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagFlag
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.AudioData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.VideoData

fun Tag.getTagFlag(): TagFlag {
    return when (getTagType()) {
        TagType.AUDIO -> (data as AudioData).tagFlag
        TagType.VIDEO -> (data as VideoData).tagFlag
        else -> TagFlag.NONE
    }
}

// region Tag 数据类型
fun Tag.isScriptTag(): Boolean =
    this.getTagType() == TagType.SCRIPT

fun Tag.isHeaderTag(): Boolean = getTagFlag() == TagFlag.HEADER

fun Tag.isEndTag(): Boolean = getTagFlag() == TagFlag.END

fun Tag.isDataTag(): Boolean {
    val tagType = getTagType()
    return tagType == TagType.AUDIO || tagType == TagType.VIDEO
}

fun Tag.isKeyframeData(): Boolean {
    return this.getTagType() == TagType.VIDEO && (this.data as VideoData).frameType == FrameType.KEY_FRAME
}
// endregion