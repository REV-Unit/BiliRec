package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.logging.BaseLogging
import moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.FlvTagProcessChain

class TagTimestampProcessNode(
    private val logger: BaseLogging,
) : BaseFlvTagProcessNode<Tag>() {
    @Volatile
    private var audioChunkTimestampDiff = -1

    @Volatile
    private var videoChunkTimestampDiff = -1

    override fun proceed(chain: FlvTagProcessChain<Tag>, tag: Tag) {
        when (tag.getTagType()) {
            TagType.AUDIO -> {
                if (audioChunkTimestampDiff <= 0) {
                    audioChunkTimestampDiff = tag.getTimeStamp() // 第一个音频分块的时间戳就是差值
                }
                tag.setTimeStamp(tag.getTimeStamp() - audioChunkTimestampDiff) // TODO: 如果出现了小于0的情况。认为有跳变，直接丢弃
            }
            TagType.VIDEO -> {
                if (videoChunkTimestampDiff <= 0) {
                    videoChunkTimestampDiff = tag.getTimeStamp() // 第一个音频分块的时间戳就是差值
                }
                tag.setTimeStamp(tag.getTimeStamp() - videoChunkTimestampDiff)
            }
            TagType.SCRIPT -> {
                if (tag.getTimeStamp() != 0) {
                    tag.setTimeStamp(0)
                }
            }
            else -> {
                // Do nothing
            }
        }
        chain.emit(tag)
    }
}