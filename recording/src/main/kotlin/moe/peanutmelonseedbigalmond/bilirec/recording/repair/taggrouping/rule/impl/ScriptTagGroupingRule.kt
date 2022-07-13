package moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.impl

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.isScriptTag
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.BaseTagGroupingRule

// ScriptTag单独一个组
class ScriptTagGroupingRule : BaseTagGroupingRule() {
    override fun canStartWith(tag: Tag): Boolean = tag.isScriptTag()

    override fun canContinueWith(tag: Tag, previousTags: List<Tag>): Boolean = false
}