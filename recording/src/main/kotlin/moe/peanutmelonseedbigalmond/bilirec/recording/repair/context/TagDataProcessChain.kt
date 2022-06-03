package moe.peanutmelonseedbigalmond.bilirec.recording.repair.context

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.tag.ScriptData
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.KeyframesObject
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.ScriptDataEcmaArray
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.ScriptDataNumber
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.value.ScriptDataObject
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.BaseFlvTagProcessChain

class TagDataProcessChain(private val logger:BaseLogging):BaseFlvTagProcessChain() {
    private var scriptChunkRead = false

    override fun proceed(tag: Tag): Tag? {
        when (tag.getTagType()) {
            TagType.SCRIPT -> {
                if (scriptChunkRead) {
                    logger.warn("Script chunk has been read, ignore this chunk")
                    return null
                }
                if ((tag.data as ScriptData)[1] is ScriptDataObject) {
                    (tag.data as ScriptData)[1] =
                        ((tag.data as ScriptData)[1] as ScriptDataObject).toScriptDataEcmaArray()
                }
                with((tag.data as ScriptData)[1] as ScriptDataEcmaArray) {
                    this["keyframes"] = KeyframesObject()
                }
                if (!containsKey(tag.data as ScriptData,"duration")){
                    with((tag.data as ScriptData)[1] as ScriptDataEcmaArray) {
                        this["duration"] = ScriptDataNumber.assign()
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
        return if (chain!= null) {
            chain!!.proceed(tag)
        } else {
            tag
        }
    }
    private fun containsKey(tag: ScriptData,key:String): Boolean {
        if (tag.size < 2 || (tag[1] !is ScriptDataEcmaArray && tag[1] !is ScriptDataObject)) return false
        val array = if (tag[1] is ScriptDataObject) {
            (tag[1] as ScriptDataObject).toScriptDataEcmaArray()
        } else {
            tag[1] as ScriptDataEcmaArray
        }
        return array.containsKey(key)
    }
}