package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum

enum class DanmakuCommand(val command: String) {
    DANMAKU("DANMU_MSG"),
    SEND_GIFT("SEND_GIFT"),
    WELCOME("WELCOME"),
    WELCOME_GUARD("WELCOME_GUARD"),
    SYSTEM_MESSAGE("SYS_MSG"),

    // 直播结束
    LIVE_END("PREPARING"),

    // 直播开始
    LIVE_START("LIVE"),
    WISH_BOTTLE("WISH_BOTTLE"),
    ROOM_CHANGE("ROOM_CHANGE"),

    // 全区广播
    NOTICE_MSG("NOTICE_MSG"),

    // 用户被禁言
    ROOM_BLOCK_MSG("ROOM_BLOCK_MSG"),

    // Super Chat
    SUPER_CHAT("SUPER_CHAT_MESSAGE"),
    SUPER_CHAT_MESSAGE_JPN("SUPER_CHAT_MESSAGE_JPN"),

    // 上舰
    GUARD_BUY("GUARD_BUY"),

    // 其余的
    OTHERS("OTHERS");

    companion object {
        fun parse(command: String): DanmakuCommand =
            values().find { it.command == command } ?: OTHERS
    }
}