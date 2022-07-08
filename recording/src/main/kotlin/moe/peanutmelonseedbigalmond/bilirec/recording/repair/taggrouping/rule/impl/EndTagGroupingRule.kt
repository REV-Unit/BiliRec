package moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.isEndTag
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.BaseTagGroupingRule

// EndTag 单独一个组
class EndTagGroupingRule : BaseTagGroupingRule() {
    override fun canStartWith(tag: Tag): Boolean=tag.isEndTag()

    override fun canContinueWith(tag: Tag, previousTags: List<Tag>): Boolean = false
}