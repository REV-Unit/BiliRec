package moe.peanutmelonseedbigalmond.bilirec.network.danmaku.data

import moe.peanutmelonseedbigalmond.bilirec.network.danmaku.enum.DanmakuCommand
import org.json.JSONObject
import java.awt.TrayIcon

// 部分字段含义参考了 https://github.com/xfgryujk/blivedm/blob/d8f7f6b7828069cb6c1fd13f756cfd891f0b1a46/blivedm/models.py
data class DanmakuModel constructor(
    /**
     * 消息类型
     */
    var command: DanmakuCommand,
    /**
     * 房间标题
     */
    var title: String? = null,
    /**
     * 直播间隶属父分区
     */
    var parentAreaName: String? = null,
    /**
     * 直播间隶属子分区
     */
    var areaName: String? = null,
    /**
     * 弹幕内容
     *
     * 当[command]为以下类型时，此字段不为 null
     * - [DanmakuCommand.DANMAKU]
     * - [DanmakuCommand.SUPER_CHAT]
     * @see DanmakuCommand
     */
    var commentText: String? = null,
    /**
     * 消息触发者用户名
     *
     * 当[command]为以下类型时，此字段不为 null
     * - [DanmakuCommand.DANMAKU]
     * - [DanmakuCommand.SEND_GIFT]
     * - [DanmakuCommand.WELCOME]
     * - [DanmakuCommand.WELCOME_GUARD]
     * - [DanmakuCommand.GUARD_BUY]
     */
    var username: String? = null,
    /**
     * 消息触发者用户ID
     *
     * 当[command]为以下类型时，此字段不为 null
     * - [DanmakuCommand.DANMAKU]
     * - [DanmakuCommand.SEND_GIFT]
     * - [DanmakuCommand.WELCOME]
     * - [DanmakuCommand.WELCOME_GUARD]
     * - [DanmakuCommand.GUARD_BUY]
     */
    var userId: Long? = null,
    /**
     * SuperChat价格
     */
    var price: Double = 0.0,
    /**
     * SuperChat显示时长
     */
    var superChatKeepTime: Int = 0,
    /**
     * 用户舰队等级
     * - 0 非船员
     * - 1 总督
     * - 2 提督
     * - 3 舰长
     *
     * 当[command]为以下类型时，此字段不为空
     * - [DanmakuCommand.DANMAKU]
     * - [DanmakuCommand.WELCOME_GUARD]
     * - [DanmakuCommand.GUARD_BUY]
     */
    var userGuardLevel: Int = 0,
    /**
     * 礼物名称
     */
    var giftName: String? = null,
    /**
     * 礼物数量
     * 当[command]为以下值时，此字段不为空
     * - [DanmakuCommand.SEND_GIFT]
     * - [DanmakuCommand.GUARD_BUY]
     *
     * 此字段也用于标识上船[DanmakuCommand.GUARD_BUY]的数量 / 月数
     */
    var giftCount: Int = 0,
    /**
     * 用户是否为房管（包括主播）
     *
     * 当[command]为以下类型时，此字段不为 null
     * - [DanmakuCommand.DANMAKU]
     * - [DanmakuCommand.SEND_GIFT]
     */
    var admin: Boolean = false,
    /**
     * 用户是否为VIP
     *
     * 当[command]为以下类型时，此字段不为 null
     * - [DanmakuCommand.DANMAKU]
     * - [DanmakuCommand.WELCOME]
     */
    var vip: Boolean = false,
    /**
     * 消息，用来判断是否是抽奖弹幕
     *
     * 对应的值为`object["info"][0][9]`
     *
     *
     * 已知值：
     *
     * 0: 普通弹幕
     * 1: 未知
     * 2: 红包
     */
    var messageType: Int = 0,
    /**
     * [DanmakuCommand.LIVE_START]，[DanmakuCommand.LIVE_END] 事件所对应的房间ID
     */
    var roomId: Long = 0L,
    /**
     * 原始数据
     */
    var rawString: String? = null,
    /**
     * 原始数据
     */
    var rawObject: JSONObject? = null,
) {
    val lotteryDanmaku: Boolean
        get() = this.messageType != 0

    companion object {
        fun fromJson(json: String): DanmakuModel {
            val obj = JSONObject(json)
            var cmd = obj.getString("cmd")
            if (cmd.startsWith("DANMU_MSG:")) {
                cmd = "DANMU_MSG"
            }
            val danmakuCommand = DanmakuCommand.parse(cmd)
            val model = DanmakuModel(danmakuCommand)
            model.rawObject = obj
            model.rawString = json
            when (danmakuCommand) {
                DanmakuCommand.LIVE_START -> model.roomId = obj.getLong("roomid")
                DanmakuCommand.LIVE_END ->
                    model.roomId = obj.getString("roomid").toLong()
                DanmakuCommand.DANMAKU -> {
                    model.commentText = obj.getJSONArray("info").getString(1)
                    model.userId = obj.getJSONArray("info").getJSONArray(2).getInt(0).toLong()
                    model.username = obj.getJSONArray("info").getJSONArray(2).getString(1)
                    model.admin = obj.getJSONArray("info").getJSONArray(2).getInt(2) == 1
                    model.vip = obj.getJSONArray("info").getJSONArray(2).getInt(3) == 1
                    model.userGuardLevel = obj.getJSONArray("info").getInt(7)
                    model.messageType = obj.getJSONArray("info").getJSONArray(0).getInt(9)
                }
                DanmakuCommand.SEND_GIFT -> {
                    model.giftName = obj.getJSONObject("data").getString("giftName")
                    model.username = obj.getJSONObject("data").getString("uname")
                    model.userId = obj.getJSONObject("data").getInt("uid").toLong()
                    model.giftCount = obj.getJSONObject("data").getInt("num")
                }
                DanmakuCommand.GUARD_BUY -> {
                    model.userId = obj.getJSONObject("data").getInt("uid").toLong()
                    model.username = obj.getJSONObject("data").getString("username")
                    model.userGuardLevel = obj.getJSONObject("data").getInt("guard_level")
                    model.giftName = when (model.userGuardLevel) {
                        3 -> "舰长"
                        2 -> "提督"
                        1 -> "总督"
                        else -> ""
                    }
                    model.giftCount = obj.getJSONObject("data").getInt("num")
                }
                DanmakuCommand.SUPER_CHAT -> {
                    model.commentText = obj.getJSONObject("data")["message"].toString()
                    model.userId = obj.getJSONObject("data").getInt("uid").toLong()
                    model.username = obj.getJSONObject("data").getJSONObject("user_info")["uname"].toString()
                    model.price = obj.getJSONObject("data").getDouble("price")
                    model.superChatKeepTime = obj.getJSONObject("data").getInt("time")
                }
                DanmakuCommand.ROOM_CHANGE -> {
                    model.title = obj.getJSONObject("data").getString("title")
                    model.areaName = obj.getJSONObject("data").getString("area_name")
                    model.parentAreaName = obj.getJSONObject("data").getString("parent_area_name")
                }
                else -> {
                    // Do nothing
                }
            }
            return model
        }
    }

    override fun toString(): String {
        return "DanmakuModel(command=$command, title=$title, parentAreaName=$parentAreaName, areaName=$areaName, commentText=$commentText, username=$username, userId=$userId, price=$price, superChatKeepTime=$superChatKeepTime, userGuardLevel=$userGuardLevel, giftName=$giftName, giftCount=$giftCount, admin=$admin, vip=$vip, lotteryDanmaku=$lotteryDanmaku, roomId=$roomId)"
    }
}