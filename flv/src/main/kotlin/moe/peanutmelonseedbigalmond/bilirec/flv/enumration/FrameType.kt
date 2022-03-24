package moe.peanutmelonseedbigalmond.bilirec.flv.enumration

enum class FrameType(val value: Int) {
    NONE(0),
    // for AVC, a seekable frame
    KEY_FRAME(1),
    // for AVC, a non-seekable frame
    INTER_FRAME(2),
    // H.263 only
    DISPOSABLE_INTER_FRAME(3),
    // reserved for server use only
    GENERATED_KEY_FRAME(4),
    // video info/command frame
    COMMAND_FRAME(5);

    companion object {
        fun fromValue(value: Int): FrameType = values().firstOrNull { it.value == value } ?: NONE
    }
}