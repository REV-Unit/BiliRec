package moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.isHeaderTag
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.BaseTagGroupingRule

// 所有连续的 HeaderTag 一个组
class HeaderTagGroupingRule : BaseTagGroupingRule() {
    override fun canStartWith(tag: Tag): Boolean = tag.isHeaderTag()

    override fun canContinueWith(tag: Tag, previousTags: List<Tag>): Boolean = tag.isHeaderTag()
}