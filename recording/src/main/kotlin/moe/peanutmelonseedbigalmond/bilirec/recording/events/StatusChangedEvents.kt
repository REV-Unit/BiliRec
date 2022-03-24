package moe.peanutmelonseedbigalmond.bilirec.recording.events

enum class StatusChangedEvents(val connected: Boolean) {
    True(true),
    False(false);

    companion object {
        fun fromValue(connected: Boolean) = if (connected) True else False
    }
}