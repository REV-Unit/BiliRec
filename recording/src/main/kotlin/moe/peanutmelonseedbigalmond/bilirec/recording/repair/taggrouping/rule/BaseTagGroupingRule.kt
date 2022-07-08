package moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.TagGroupingProcessChain

/**
 * Tag 分组规则，参考自 [来源](https://rec.danmuji.org/dev/repair/#tag-%E5%88%86%E7%BB%84)
 */
abstract class BaseTagGroupingRule {
    abstract fun canStartWith(tag: Tag): Boolean
    abstract fun canContinueWith(tag: Tag, previousTags: List<Tag>): Boolean
}