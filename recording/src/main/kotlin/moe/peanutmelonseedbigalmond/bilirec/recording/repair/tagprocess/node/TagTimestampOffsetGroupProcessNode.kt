package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.middleware.Middleware
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareContext
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareNext
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import java.util.*

// https://github.com/BililiveRecorder/BililiveRecorder/blob/dev/BililiveRecorder.Flv/Pipeline/Rules/UpdateTimestampOffsetRule.cs
/**
 * 修复时间戳错位
 * 似乎由于B站传回的数据第一个分块问题，此规则不再适用
 * 新的处理逻辑在 [UpdateTagTimestampGroupProcessNode.execute] 中
 */

@Deprecated("This rule no longer applicable")
class TagTimestampOffsetGroupProcessNode : Middleware<TagGroup> {
    // 判断指定列表中是否存在指定两个连续的元素符合指定的条件
    private inline fun <T> List<T>.anyTwoElementMeets(predicate: (element1: T, element2: T) -> Boolean): Boolean {
        if (this.isEmpty()) return false

        val iterator = iterator()

        var lastElement = iterator.next()

        for (currentElement in iterator) {
            if (predicate(lastElement, currentElement)) return true

            lastElement = currentElement
        }
        return false
    }

    // 判断 Tag 时间戳是否存在减小的情况
    private fun tagTimestampDecrease(tag1: Tag, tag2: Tag): Boolean = tag1.getTimeStamp() > tag2.getTimeStamp()

    private fun reduceOffsetRange(
        oldMaxOffset: Int,
        oldMinOffset: Int,
        leftAudioTag: Tag?,
        rightAudioTag: Tag?,
        tags: List<Tag>
    ): ReduceOffsetRangeResult {
        val tagsStack = Stack<Tag>().also { it.addAll(tags) }
        var min = oldMinOffset
        var max = oldMaxOffset
        while (tagsStack.isNotEmpty()) {
            val video = tagsStack.pop()

            if (leftAudioTag != null) {
                min = (leftAudioTag.getTimeStamp() - video.getTimeStamp())
                    .coerceAtLeast(min)
            }

            if (rightAudioTag != null) {
                max = (rightAudioTag.getTimeStamp() - video.getTimeStamp())
                    .coerceAtMost(max)
            }
        }

        return ReduceOffsetRangeResult(min, max, tagsStack.toMutableList())
    }

    private data class ReduceOffsetRangeResult(
        val newMinOffset: Int,
        val newMaxOffset: Int,
        val newTags: MutableList<Tag>,
    )

    override fun execute(context: MiddlewareContext<TagGroup, *>, next: MiddlewareNext) {
        val tagGroup = context.data
        val abnormal = tagGroup.anyTwoElementMeets(::tagTimestampDecrease)

        if (!abnormal) { // 如果所有的时间戳都是递增的，没有问题
            return next.execute()
        }

        val audioTagList = tagGroup.filter { it.getTagType() == TagType.AUDIO }
        val videoTagList = tagGroup.filter { it.getTagType() == TagType.VIDEO }
        if (audioTagList.anyTwoElementMeets(::tagTimestampDecrease) || videoTagList.anyTwoElementMeets(::tagTimestampDecrease)) {
            // 音频/视频时间戳序列有问题
            throw Exception("Tag 时间戳序列异常")
        } else {
            /**
             * 设定做调整的为视频帧，参照每个视频帧左右（左为前、右为后）的音频帧的时间戳
             *
             * 计算出最多和最少能符合“不小于前面的帧并且不大于后面的帧”的要求的偏移量
             *
             * 如果当前偏移量比总偏移量要求更严，则使用当前偏移量范围作为总偏移量范围
             */

            val offset: Int

            var lastAudioTag: Tag? = null

            var tags = mutableListOf<Tag>()
            var minOffset: Int = Int.MIN_VALUE
            var maxOffset: Int = Int.MAX_VALUE

            tagGroup.forEach {
                when (it.getTagType()) {
                    TagType.AUDIO -> {
                        reduceOffsetRange(maxOffset, minOffset, lastAudioTag, it, tags).also { res ->
                            maxOffset = res.newMaxOffset
                            minOffset = res.newMinOffset
                            tags = res.newTags
                        }

                        lastAudioTag = it
                    }
                    TagType.VIDEO -> tags.add(it)
                    else -> throw IllegalArgumentException("unexpected tag type: ${it.getTagType()}")
                }
            }

            reduceOffsetRange(maxOffset, minOffset, lastAudioTag, null, tags).also { res ->
                maxOffset = res.newMaxOffset
                minOffset = res.newMinOffset
                tags = res.newTags
            }

            if (minOffset == maxOffset) {
                // 理想情况允许偏移范围只有一个值
                offset = minOffset

                context.data.clear()
                context.data.addAll(onValidOffset(offset, tags).toList().flatten())
                return next.execute()
            } else if (minOffset < maxOffset) {
                // 允许偏移的值是一个范围
                if (minOffset != Int.MAX_VALUE) {
                    if (maxOffset != Int.MIN_VALUE) {
                        // 有一个有效范围，取平均值
                        offset = (minOffset + maxOffset) / 2

                        context.data.clear()
                        context.data.addAll(onValidOffset(offset, tags).toList().flatten())
                        return next.execute()
                    } else {
                        // 无效最大偏移，以最小偏移为准
                        offset = minOffset + 1

                        context.data.clear()
                        context.data.addAll(onValidOffset(offset, tags).toList().flatten())
                        return next.execute()
                    }
                } else {
                    if (maxOffset != Int.MAX_VALUE) {
                        // 无效最小偏移，以最大偏移为准
                        offset = maxOffset - 1

                        context.data.clear()
                        context.data.addAll(onValidOffset(offset, tags).toList().flatten())
                        return next.execute()
                    } else {
                        // 无效结果
                        onInvalidOffset()
                    }
                }
            } else {
                // 范围无效
                onInvalidOffset()
            }
        }
    }

    private fun onValidOffset(
        offset: Int,
        tags: TagGroup
    ) = sequence {
        if (offset != 0) {
            tags.forEach {
                if (it.getTagType() == TagType.VIDEO) {
                    it.setTimeStamp(it.getTimeStamp() + offset)
                }
            }
            yield(tags)
        }
    }

    private fun onInvalidOffset(): Nothing {
        throw IllegalArgumentException("Invalid tag timestamp offset")
    }
}