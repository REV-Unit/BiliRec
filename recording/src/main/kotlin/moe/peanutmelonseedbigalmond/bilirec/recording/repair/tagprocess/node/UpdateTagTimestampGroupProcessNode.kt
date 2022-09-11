package moe.peanutmelonseedbigalmond.bilirec.recording.repair.tagprocess.node

import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagFlag
import moe.peanutmelonseedbigalmond.bilirec.flv.enumration.TagType
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.Tag
import moe.peanutmelonseedbigalmond.bilirec.flv.strcture.getTagFlag
import moe.peanutmelonseedbigalmond.bilirec.middleware.Middleware
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareContext
import moe.peanutmelonseedbigalmond.bilirec.middleware.MiddlewareNext
import moe.peanutmelonseedbigalmond.bilirec.recording.TagGroup
import kotlin.math.max

// 修复时间戳跳变
class UpdateTagTimestampGroupProcessNode : Middleware<TagGroup> {
    companion object {
        private const val TS_STORE_KEY = "TimestampStoreKey"
        private const val JUMP_THRESHOLD = 50

        private const val AUDIO_DURATION_FALLBACK = 22
        private const val AUDIO_DURATION_MIN = 20
        private const val AUDIO_DURATION_MAX = 24

        private const val VIDEO_DURATION_FALLBACK = 33
        private const val VIDEO_DURATION_MIN = 15
        private const val VIDEO_DURATION_MAX = 50
    }

    override fun execute(context: MiddlewareContext<TagGroup, *>, next: MiddlewareNext) {
        @Suppress("UNCHECKED_CAST")
        val extra = context.extra as MutableMap<Any, Any>
        val timestampStore = extra[TS_STORE_KEY] as TimeStampStore? ?: TimeStampStore()
        extra[TS_STORE_KEY] = timestampStore
        val tags = context.data

        if (tags[0].getTagType() == TagType.SCRIPT) {
            // Script Tag时间戳永远为 0
            tags[0].setTimeStamp(0)
            return next.execute()
        }

        if (tags.all { it.getTagFlag() == TagFlag.HEADER }) {
            // Header Tag 时间戳永远为 0
            tags.forEach { it.setTimeStamp(0) }
            timestampStore.reset()
            return next.execute()
        }

        var currentTimestamp = tags[0].getTimeStamp()

        val isFirstChunk = timestampStore.firstChunk
        if (isFirstChunk) {
            // 第一段数据使用最小的时间戳作为基础偏移量
            // 防止出现前几个 Tag 时间戳为负数的情况
            timestampStore.firstChunk = false
            currentTimestamp = tags.minOf { it.getTimeStamp() }
        }

        val diff = currentTimestamp - timestampStore.lastOriginalTimestamp

        if (diff < -JUMP_THRESHOLD || isFirstChunk && (diff < 0)) {
            context.logger.debug("时间戳变小, current=$currentTimestamp, diff=$diff")
            timestampStore.currentOffset = currentTimestamp - timestampStore.nextTimestampTarget
        } else if (diff > JUMP_THRESHOLD) {
            context.logger.debug("时间戳变大, current=$currentTimestamp, diff=$diff")
            timestampStore.currentOffset = currentTimestamp - timestampStore.nextTimestampTarget
        }

        timestampStore.lastOriginalTimestamp = tags.last().getTimeStamp()

        tags.forEach { it.setTimeStamp(it.getTimeStamp() - timestampStore.currentOffset) }

        timestampStore.nextTimestampTarget = this.calculateNewTargetTimestamp(tags)
        return next.execute()
    }

    private fun calculateNewTargetTimestamp(tags: List<Tag>): Int {
        var video = 0
        var audio = 0

        if (tags.any { it.getTagType() == TagType.VIDEO }) {
            video = calculatePerChannel(
                tags,
                VIDEO_DURATION_FALLBACK,
                VIDEO_DURATION_MAX,
                VIDEO_DURATION_MIN,
                TagType.VIDEO
            )
        }

        if (tags.any { it.getTagType() == TagType.AUDIO }) {
            audio = calculatePerChannel(
                tags,
                AUDIO_DURATION_FALLBACK,
                AUDIO_DURATION_MAX,
                AUDIO_DURATION_MIN,
                TagType.AUDIO
            )
        }

        return max(video, audio)
    }

    private fun calculatePerChannel(tags: List<Tag>, fallback: Int, max: Int, min: Int, tagType: TagType): Int {
        val sample = tags.filter { it.getTagType() == tagType }.take(2).toTypedArray()
        var durationPreTag: Int
        if (sample.size != 2) {
            durationPreTag = fallback
        } else {
            durationPreTag = sample[1].getTimeStamp() - sample[0].getTimeStamp()

            if (durationPreTag !in min..max) {
                durationPreTag = fallback
            }
        }

        return durationPreTag + tags.last { it.getTagType() == tagType }.getTimeStamp()
    }

    private class TimeStampStore {
        var nextTimestampTarget = 0
        var lastOriginalTimestamp = 0
        var currentOffset = 0
        var firstChunk = false

        init {
            reset()
        }

        fun reset() {
            this.firstChunk = true
            this.currentOffset = 0
            this.lastOriginalTimestamp = 0
            this.nextTimestampTarget = 0
        }
    }
}