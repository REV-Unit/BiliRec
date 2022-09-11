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
        val tagGroup = context.data
        if (tagGroup[0].getTagType() == TagType.SCRIPT) {
            // Script Tag时间戳永远为 0
            tagGroup[0].setTimeStamp(0)

            return next.execute()
        }

        if (tagGroup.all { it.getTagFlag() == TagFlag.HEADER }) {
            // Header Tag 时间戳永远为 0
            tagGroup.forEach { it.setTimeStamp(0) }
            return next.execute()
        }

        @Suppress("UNCHECKED_CAST")
        val nodeItems = context.extra as MutableMap<Any, Any>

        val ts = (nodeItems[TS_STORE_KEY] as TimeStampStore?) ?: TimeStampStore()

        val currentTimestamp = tagGroup.first().getTimeStamp()
        val diff = currentTimestamp - ts.lastOriginalTimestamp
        if (diff < 0) {
            context.logger.trace("时间戳变小, current: $currentTimestamp, diff: $diff")
            ts.currentOffset = currentTimestamp - ts.nextTimestampTarget
        } else if (diff > JUMP_THRESHOLD) {
            context.logger.trace("时间戳变化过大, current: $currentTimestamp, diff: $diff")
            ts.currentOffset = currentTimestamp - ts.nextTimestampTarget
        }

        ts.lastOriginalTimestamp = tagGroup.last().getTimeStamp()

        tagGroup.forEach { it.setTimeStamp(it.getTimeStamp() - ts.currentOffset) }

        ts.nextTimestampTarget = calculateNewTargetTimestamp(tagGroup)

        nodeItems[TS_STORE_KEY] = ts

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

        fun reset() {
            this.currentOffset = 0
            this.lastOriginalTimestamp = 0
            this.nextTimestampTarget = 0
        }
    }
}