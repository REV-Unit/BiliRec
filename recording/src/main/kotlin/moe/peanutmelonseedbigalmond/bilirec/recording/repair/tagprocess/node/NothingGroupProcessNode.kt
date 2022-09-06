package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup

// 不做任何处理
class NothingGroupProcessNode : BaseFlvTagGroupProcessNode() {
    override fun proceed(tagGroup: TagGroup) = sequence {
        yieldAll(belongsToChain.dataSource)
    }
}