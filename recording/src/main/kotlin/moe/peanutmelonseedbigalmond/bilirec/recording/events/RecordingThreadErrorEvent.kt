package moe.peanutmelonseedbigalmond.bilirec.recording.events

import moe.peanutmelonseedbigalmond.bilirec.recording.Room

data class RecordingThreadErrorEvent(
    val room: Room,
    val extra: Any?,
)