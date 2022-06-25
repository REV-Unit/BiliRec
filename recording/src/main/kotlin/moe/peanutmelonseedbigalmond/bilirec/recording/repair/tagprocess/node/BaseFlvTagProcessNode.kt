package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain

abstract class BaseFlvTagProcessNode<R> {
    var tailNode: BaseFlvTagProcessNode<R>? = null
    abstract fun proceed(chain: FlvTagProcessChain<R>, tag: R)

    protected open fun next(chain: FlvTagProcessChain<R>, tag: R) {
        if (this.tailNode != null) {
            this.tailNode!!.proceed(chain, tag)
        } else {
            chain.emit(tag)
        }
    }
}