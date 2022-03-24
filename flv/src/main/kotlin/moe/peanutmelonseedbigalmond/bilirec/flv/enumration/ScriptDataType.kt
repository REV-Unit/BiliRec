package moe.peanutmelonseedbigalmond.bilirec.flv.enumration

enum class ScriptDataType(val value: Int) {
    UNKNOWN(-1),
    NUMBER(0),
    BOOLEAN(1),
    STRING(2),
    OBJECT(3),
    MOVE_CLIP(4),
    NULL(5),
    UNDEFINED(6),
    REFERENCE(7),
    ECMA_ARRAY(8),
    OBJECT_END_MARKER(9),
    STRICT_ARRAY(10),
    DATE(11),
    LONG_STRING(12);

    companion object {
        fun fromValue(value: Int) = values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}