package moe.peanutmelonseedbigalmond.bilirec.recording.events

import java.time.Duration
import java.time.OffsetDateTime

data class NetworkStatusEvents(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val duration: Duration,
    val bytesDownloaded: Long,
    val bitrate: Double,
)