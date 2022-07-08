package moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagFlag
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.getTagFlag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.isDataTag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.isKeyframeData
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.BaseTagGroupingRule

/**
 * 每个 GOP 一个组
 */
class GOPGroupingRule : BaseTagGroupingRule() {
    override fun canStartWith(tag: Tag): Boolean = tag.isDataTag()

    override fun canContinueWith(tag: Tag, previousTags: List<Tag>): Boolean = !tag.isKeyframeData()
            || (
            tag.getTagType() == TagType.AUDIO
                    && tag.getTagFlag() == TagFlag.HEADER
                    && previousTags.all(::tagNotAudioTagAndTagIsHeader)
            )

    private fun tagNotAudioTagAndTagIsHeader(tag: Tag): Boolean {
        return tag.getTagType() != TagType.AUDIO || tag.getTagFlag() == TagFlag.HEADER
    }
}