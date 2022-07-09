package moe.peanutmelonseedbigalmond.bilirec.web.bean.request

import moe.peanutmelonseedbigalmond.bilirec.config.RoomConfig

data class AddRoomRequest(
    var roomId: Long? = null,
    var enableAutoRecord: Boolean? = null,
    var recordRawDanmakuData: Boolean? = null,
    var recordGuardByData: Boolean? = null,
    var recordSendGiftData: Boolean? = null,
    var recordSuperChatData: Boolean? = null,
    var filterLotteryDanmaku: Boolean? = null,
    var danmakuFilterRegex: ArrayList<String>? = null,
    var recordMode: Int? = null,
) {
    fun toRoomConfig(): RoomConfig {
        requireNotNull(roomId)
        require(roomId != 0L){ "Room id cannot be 0"}
        return RoomConfig().also { config ->
            config.roomId = roomId!!
            enableAutoRecord?.let { config.enableAutoRecord = it }
            recordRawDanmakuData?.let { config.recordRawDanmakuData = it }
            recordGuardByData?.let { config.recordGuardByData = it }
            recordSendGiftData?.let { config.recordSendGiftData = it }
            recordSuperChatData?.let { config.recordSuperChatData = it }
            filterLotteryDanmaku?.let { config.filterLotteryDanmaku = it }
            danmakuFilterRegex?.let { config.danmakuFilterRegex = it }
            recordMode?.let { config.recordMode = it }
        }
    }
}