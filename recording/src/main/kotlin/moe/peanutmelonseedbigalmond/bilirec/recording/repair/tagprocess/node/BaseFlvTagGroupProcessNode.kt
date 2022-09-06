package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagGroupProcessChain

abstract class BaseFlvTagGroupProcessNode {
    lateinit var belongsToChain: FlvTagGroupProcessChain
    private val myPrevNode: BaseFlvTagGroupProcessNode?
        get() = belongsToChain.getPrevNode(this)

    protected abstract fun proceed(tagGroup: TagGroup): Sequence<TagGroup>

    fun readNextProceedTagGroups(): Sequence<TagGroup> = sequence {
        if (myPrevNode == null) { // 已经是第一个节点
            val d=belongsToChain.dataSource.toList()
            d.forEach {
                proceed(it).forEach { tg ->
                    yield(tg)
                }
            }
        } else {
            val prevProceed = myPrevNode!!.readNextProceedTagGroups()
            for (item in prevProceed) {
                proceed(item).forEach {
                    yield(it)
                }
            }
        }
    }
}