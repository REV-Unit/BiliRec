package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess

import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node.BaseFlvTagGroupProcessNode
import java.util.*

class FlvTagGroupProcessChain private constructor() {
    private val nodes = LinkedList<BaseFlvTagGroupProcessNode>()
    var logger: BaseLogging = LoggingFactory.getLogger(obj = this)
        private set
    lateinit var dataSource: Sequence<TagGroup>
        private set

    // 用来存放各个节点产生的数据，以便在节点间共享
    val nodeItems = mutableMapOf<Any, Any?>()

    fun readTagGroupList(): Sequence<TagGroup> {
        return nodes.last.readNextProceedTagGroups()
    }

    fun getPrevNode(node: BaseFlvTagGroupProcessNode): BaseFlvTagGroupProcessNode? {
        val prevIndex = nodes.indexOf(node) - 1
        return if (prevIndex < 0) null else nodes[prevIndex]
    }

    class Builder {
        private val chain = FlvTagGroupProcessChain()
        fun addNode(node: BaseFlvTagGroupProcessNode): Builder {
            node.belongsToChain = this.chain
            chain.nodes.add(node)
            return this
        }

        fun setLogger(logger: BaseLogging):Builder{
            chain.logger=logger
            return this
        }

        fun setDataSource(dataSource: Sequence<TagGroup>): Builder {
            chain.dataSource = dataSource
            return this
        }

        fun build(): FlvTagGroupProcessChain {
            return this.chain
        }
    }
}