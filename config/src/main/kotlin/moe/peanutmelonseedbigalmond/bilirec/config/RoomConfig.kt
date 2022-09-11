package moe.peanutmelonseedbigalmond.bilirec.config

@kotlinx.serialization.Serializable
class RoomConfig {
    @Volatile
    var roomId: Long = 0

    @Volatile
    var enableAutoRecord: Boolean = true

    @Volatile
    var recordRawDanmakuData: Boolean = true

    @Volatile
    var recordGuardByData: Boolean = true

    @Volatile
    var recordSendGiftData: Boolean = true

    @Volatile
    var recordSuperChatData: Boolean = true

    @Volatile
    var filterLotteryDanmaku: Boolean = true

    @Volatile
    var danmakuFilterRegex: ArrayList<String> = arrayListOf()

    @Volatile
    var title: String = ""

    @Volatile
    var recordMode: Int = RecordMode.STRAND

    object RecordMode {
        const val DIAGNOSIS = -1
        const val STRAND = 0
        const val RAW = 1
    }
}