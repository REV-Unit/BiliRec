package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.*
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagGroupProcessChain

class ScriptTagNormalizeGroupProcessNode : BaseFlvTagGroupProcessNode() {
    companion object {
        private const val STORE_KEY = "MetaDataReceived"
        private const val KEY_FRAMES_KEY = "keyframes"
        private const val DURATION_KEY = "duration"
    }

    // 根据分组规则，ScriptTag 单独一组，所以 tag 有且仅有一个元素
    override fun proceed(tagGroup: TagGroup) = sequence {
        val t = tagGroup[0]
        if (t.getTagType() == TagType.SCRIPT) {
            val data = t.data as ScriptData

            if (data.size == 2 && (data[0] is ScriptDataString) && (data[0] as ScriptDataString).value == "onMetaData") {
                return@sequence yield(onMetaData(t).toList())
            } else if (data.size == 3 && data[2] is ScriptDataNull && data[0] is ScriptDataString && (data[0] as ScriptDataString).value == "onMetaData") {
                return@sequence yield(onMetaData(t).toList())
            } else {
                yieldAll(onNotMetaData(tagGroup))
            }
        } else {
            yieldAll(onNotMetaData(tagGroup))
        }
    }

    private fun onMetaData(tag: Tag) = sequence {
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
        value[DURATION_KEY] = ScriptDataNumber.assign()

        data[1] = value

        val metaDataReceived = (belongsToChain.nodeItems[STORE_KEY] as Boolean?) ?: false
        if (!metaDataReceived) {
            belongsToChain.nodeItems[STORE_KEY] = true

            val newTag = Tag().also {
                it.setTagType(TagType.SCRIPT)
                it.data = data
            }
            return@sequence yield(newTag)
        } else {
            belongsToChain.logger.warn("收到了重复的metaData数据")

            // 当前的处理逻辑为：直接忽略掉新的 Script Tag
            return@sequence

//            val newTag = Tag().also {
//                it.setTagType(TagType.SCRIPT)
//                it.data = data
//            }
//            return next(chain, mutableListOf(newTag))
        }
    }

    private fun onNotMetaData(tags: TagGroup)= sequence {
        yield(tags)
    }
}