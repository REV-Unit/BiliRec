package moe.peanutmelonseedbigalmond.bilirec.flv.enumration

enum class TagType(val value: Int) {
    AUDIO(0x8),
    VIDEO(0x9),
    SCRIPT(0x12),
    UNKNOWN(0x0);

    companion object {
        fun fromValue(value: Int) = values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}