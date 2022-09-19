package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.*
import moe.peanutmelonseedbigalmond.bilirec.middleware.Middleware
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareContext
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareNext
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup

class ScriptTagNormalizeGroupProcessNode : Middleware<TagGroup> {
    companion object {
        private const val STORE_KEY = "MetaDataReceived"
        private const val KEY_FRAMES_KEY = "keyframes"
        private const val DURATION_KEY = "duration"
    }

    // 根据分组规则，ScriptTag 单独一组，所以 tag 有且仅有一个元素
    override fun execute(context: MiddlewareContext<TagGroup, *>, next: MiddlewareNext) {
        val tagGroup = context.data
        val t = tagGroup[0]
        if (t.getTagType() == TagType.SCRIPT) {
            val data = t.data as ScriptData

            // TODO: 当重复收到onMetaData的时候，会在之后的处理节点抛出越界异常，强行中断当前录制，使录播文件分段
            // TODO: 应采用更友好的方式分段
            if (data.size == 2 && (data[0] is ScriptDataString) && (data[0] as ScriptDataString).value == "onMetaData") {
                context.data.clear()
                context.data.addAll(onMetaData(t, context).toList())
                return next.execute()
            } else if (data.size == 3 && data[2] is ScriptDataNull && data[0] is ScriptDataString && (data[0] as ScriptDataString).value == "onMetaData") {
                context.data.clear()
                context.data.addAll(onMetaData(t, context).toList())
                return next.execute()
            } else {
                return next.execute()
            }
        } else {
            return next.execute()
        }
    }


    private fun onMetaData(tag: Tag, context: MiddlewareContext<TagGroup, *>) = sequence {
        val data = tag.data as ScriptData

        @Suppress("UNCHECKED_CAST")
        val nodeItems = context.extra as MutableMap<Any, Any>

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

        val metaDataReceived = (nodeItems[STORE_KEY] as Boolean?) ?: false
        if (!metaDataReceived) {
            nodeItems[STORE_KEY] = true

            val newTag = Tag().also {
                it.setTagType(TagType.SCRIPT)
                it.data = data
            }
            yield(newTag)
        } else {
            context.logger.warn("收到了重复的metaData数据")

            // 当前的处理逻辑为：直接忽略掉新的 Script Tag
            return@sequence

//            val newTag = Tag().also {
//                it.setTagType(TagType.SCRIPT)
//                it.data = data
//            }
//            return next(chain, mutableListOf(newTag))
        }
    }
}