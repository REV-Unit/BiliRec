package moe.peanutmelonseedbigalmond.bilirec.config

class RoomConfig {
    @Volatile
    var roomId: Long=0

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
    var filterLotteryDanmaku:Boolean=true

    @Volatile
    var danmakuFilterRegex:ArrayList<String> = arrayListOf()

    @Volatile
    var title: String = ""
}