package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess

import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.BaseFlvTagProcessNode

class FlvTagProcessChain<R>(val logger: BaseLogging) {
    private lateinit var collectAction: (R) -> Unit
    private var chainFirstNode: BaseFlvTagProcessNode<R>? = null
    private var chainLastNode: BaseFlvTagProcessNode<R>? = null

    // 用来存放各个节点产生的数据，以便在节点间共享
    val nodeItems = mutableMapOf<Any, Any?>()

    fun addProcessNode(node: BaseFlvTagProcessNode<R>): FlvTagProcessChain<R> {
        if (chainFirstNode == null) {
            chainFirstNode = node
            chainLastNode = node
        } else {
            chainLastNode?.tailNode = node
            chainLastNode = node
        }
        return this
    }

    fun collect(action: (R) -> Unit): FlvTagProcessChain<R> {
        collectAction = action
        return this
    }

    /**
     * 将数据交给第一个节点，开始处理
     * 如果没有处理节点，则直接从链上返回
     */
    fun startProceed(obj: R) {
        if (chainFirstNode != null) {
            chainFirstNode!!.proceed(this, obj)
        } else {
            emit(obj)
        }
    }

    /**
     * 直接跳出处理链，数据不再往下一个节点传播
     */
    fun emit(obj: R) {
        collectAction(obj)
    }
}