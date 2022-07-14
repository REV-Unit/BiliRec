package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain

class UpdateTagTimestampProcessNode : BaseFlvTagProcessNode<List<Tag>>() {
    override fun proceed(chain: FlvTagProcessChain<List<Tag>>, tag: List<Tag>) {
        chain.emit(tag)
    }
}