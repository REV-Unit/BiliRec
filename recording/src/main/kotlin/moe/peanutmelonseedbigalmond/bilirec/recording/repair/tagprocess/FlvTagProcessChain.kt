package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess

import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.BaseFlvTagProcessNode
import java.util.*

class FlvTagProcessChain<R> {
    private lateinit var collectAction: (R) -> Unit
    private val processNodes: LinkedList<BaseFlvTagProcessNode<R>> = LinkedList()
    private lateinit var iterator: MutableIterator<BaseFlvTagProcessNode<R>>

    fun addProcessNode(node: BaseFlvTagProcessNode<R>): FlvTagProcessChain<R> {
        processNodes.add(node)
        return this
    }

    fun collect(action: (R) -> Unit): FlvTagProcessChain<R> {
        collectAction = action
        return this
    }

    fun proceed(obj: R) {
        processNodes.first.proceed(this, obj)
    }

    fun emit(obj: R) {
        ensureIteratorInitialized()
        if (iterator.hasNext()) {
            iterator.next().proceed(this, obj)
        } else {
            collectAction(obj)
        }
    }

    private fun ensureIteratorInitialized() {
        if (this::iterator.isInitialized) return
        iterator = processNodes.iterator()
    }
}