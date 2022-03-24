package moe.peanutmelonseedbigalmond.bilirec.recording.repair

import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag

abstract class BaseFlvTagProcessChain {
    protected var chain: BaseFlvTagProcessChain? = null

    fun setNextChainNode(nextChainNode: BaseFlvTagProcessChain) {
        this.chain = nextChainNode
    }

    abstract fun proceed(tag: Tag): Tag?

    companion object Builder {
        private lateinit var head: BaseFlvTagProcessChain
        private lateinit var tail: BaseFlvTagProcessChain

        fun addChainNode(node: BaseFlvTagProcessChain): Builder {
            if (!::head.isInitialized) {
                head = node
                tail = node
            } else {
                tail.setNextChainNode(node)
                tail = node
            }
            return this
        }

        fun build(): BaseFlvTagProcessChain {
            return head
        }
    }
}