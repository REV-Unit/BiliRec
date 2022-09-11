package moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.taggrouping.rule.BaseTagGroupingRule

class TagGroupingRuleChain private constructor() {
    lateinit var logger: BaseLogging
        private set
    private val tagGroup by lazy { mutableListOf<Tag>() }
    private val rules: MutableList<BaseTagGroupingRule> by lazy { ArrayList() }
    private var leftoverTag: Tag? = null
    private var ruleCanHandlePreviousTag: BaseTagGroupingRule? = null
    private var onTagGroupEmitted: ((TagGroup) -> Unit)? = null

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
        } else {
            if (ruleCanHandlePreviousTag!!.canContinueWith(tag, tagGroup)) {
                tagGroup.add(tag)
            } else {
                onTagGroupEmitted?.invoke(tagGroup)
                tagGroup.clear()
                leftoverTag = tag
            }
        }
    }

    fun finally() {
        if (leftoverTag != null) {
            onTagGroupEmitted?.invoke(mutableListOf(leftoverTag!!))
        }
    }

    private fun findRuleCanStartWith(tag: Tag): BaseTagGroupingRule? {
        return rules.firstOrNull { it.canStartWith(tag) }
    }

    class Builder {
        private val chain = TagGroupingRuleChain()
        fun setLogger(logger: BaseLogging): Builder {
            chain.logger = logger
            return this
        }

        fun addRule(rule: BaseTagGroupingRule): Builder {
            chain.rules.add(rule)
            return this
        }

        fun build(): TagGroupingRuleChain {
            return chain
        }

        fun onTagGroupEmitted(onTagGroupEmitted: (TagGroup) -> Unit): Builder {
            chain.onTagGroupEmitted = onTagGroupEmitted
            return this
        }
    }
}