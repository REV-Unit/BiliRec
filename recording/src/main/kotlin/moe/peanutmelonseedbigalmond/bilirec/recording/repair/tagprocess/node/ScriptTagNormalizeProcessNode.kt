package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.*
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain

class ScriptTagNormalizeProcessNode : BaseFlvTagProcessNode<List<Tag>>() {
    companion object {
        private const val STORE_KEY = "MetaDataReceived"
        private const val KEY_FRAMES_KEY = "keyframes"
        private const val DURATION_KEY="duration"
    }

    // 根据分组规则，ScriptTag 单独一组，所以 tag 有且仅有一个元素
    override fun proceed(chain: FlvTagProcessChain<List<Tag>>, tag: List<Tag>) {
        val t = tag[0]
        return if (t.getTagType() == TagType.SCRIPT) {
            val data = t.data as ScriptData

            return if (data.size == 2 && (data[0] is ScriptDataString) && (data[0] as ScriptDataString).value == "onMetaData") {
                onMetaData(chain, t)
            } else if (data.size == 3 && data[2] is ScriptDataNull && data[0] is ScriptDataString && (data[0] as ScriptDataString).value == "onMetaData") {
                onMetaData(chain, t)
            } else {
                onNotMetaData(chain, tag)
            }
        } else {
            onNotMetaData(chain, tag)
        }
    }

    private fun onMetaData(chain: FlvTagProcessChain<List<Tag>>, tag: Tag) {
        val data = tag.data as ScriptData
        // 将 metaData 中的数据存储结构全部换成 array
        val value = when (val d = data[1]) {
            is ScriptDataObject -> d.toScriptDataEcmaArray()
            is ScriptDataEcmaArray -> d
            else -> ScriptDataEcmaArray()
        }

        // 加一个存储关键帧的数据结构
        value[KEY_FRAMES_KEY] = KeyframesObject()

        // 加一个存储视频总长度的位置
        value[DURATION_KEY]=ScriptDataNumber.assign()

        data[1] = value

        val metaDataReceived = (chain.nodeItems[STORE_KEY] as Boolean?) ?: false
        if (!metaDataReceived) {
            chain.nodeItems[STORE_KEY] = true

            val newTag = Tag().also {
                it.setTagType(TagType.SCRIPT)
                it.data = data
            }
            return next(chain, mutableListOf(newTag))
        } else {
            chain.logger.warn("收到了重复的metaData数据")

            val newTag = Tag().also {
                it.setTagType(TagType.SCRIPT)
                it.data = data
            }
            return next(chain, mutableListOf(newTag))
        }
    }

    private fun onNotMetaData(chain: FlvTagProcessChain<List<Tag>>, tags: List<Tag>) {
        return next(chain, tags)
    }
}