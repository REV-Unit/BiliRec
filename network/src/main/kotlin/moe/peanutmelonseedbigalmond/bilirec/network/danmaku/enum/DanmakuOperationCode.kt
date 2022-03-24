package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum

enum class DanmakuOperationCode(val code: Int) {
    HEART_BEAT(2),
    POPULARITY(3),
    COMMAND(5),
    ENTER_ROOM(7),
    HEART_BEAT_FROM_SERVER(8),
    OTHERS(-1);

    companion object {
        fun parse(value: Int): DanmakuOperationCode =
            values().find { it.code == value } ?: OTHERS
    }
}