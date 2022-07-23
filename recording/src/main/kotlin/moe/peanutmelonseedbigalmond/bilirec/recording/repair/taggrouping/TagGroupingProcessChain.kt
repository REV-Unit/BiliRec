package moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.BaseTagGroupingRule

class TagGroupingProcessChain(val logger: BaseLogging) {
    private lateinit var collectAction: (List<Tag>) -> Unit
    private val tagGroup: MutableList<Tag> by lazy { ArrayList() }
    private val rules: MutableList<BaseTagGroupingRule> by lazy { ArrayList() }
    private var leftoverTag: Tag? = null
    private var ruleCanHandlePreviousTag: BaseTagGroupingRule? = null

    fun addRule(rule: BaseTagGroupingRule): TagGroupingProcessChain {
        rules.add(rule)
        return this
    }

    fun collect(action: (List<Tag>) -> Unit): TagGroupingProcessChain {
        this.collectAction = action
        return this
    }

    fun proceed(tag: Tag) {
        if (leftoverTag != null) {
            ruleCanHandlePreviousTag = findRuleCanStartWith(leftoverTag!!)
                ?: throw UnsupportedOperationException("No rule can handle with this tag: $leftoverTag")
            tagGroup.add(leftoverTag!!)
            leftoverTag = null
        }
        if (ruleCanHandlePreviousTag == null) {
            ruleCanHandlePreviousTag = findRuleCanStartWith(tag)
                ?: throw UnsupportedOperationException("No rule can handle with this tag: $tag")
            tagGroup.add(tag)
            return
        } else {
            if (ruleCanHandlePreviousTag!!.canContinueWith(tag, tagGroup)) {
                tagGroup.add(tag)
            } else {
                emit()
                leftoverTag = tag
            }
        }
    }

    private fun findRuleCanStartWith(tag: Tag): BaseTagGroupingRule? {
        return rules.firstOrNull { it.canStartWith(tag) }
    }

    fun emit() {
        try {
            collectAction(tagGroup)
        } finally {
            tagGroup.clear()
        }
    }
}