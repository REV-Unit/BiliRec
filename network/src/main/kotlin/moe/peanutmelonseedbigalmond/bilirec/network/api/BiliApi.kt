package moe.peanutmelonseedbigalmond.bilirec.network.api

import com.haroldadmin.cnradapter.NetworkResponse
import moe.peanutmelonseedbigalmond.bilirec.network.api.response.*
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface BiliApi {
    @GET("/room/v1/Room/get_info")
    suspend fun getRoomInfoAsync(
        @Query("id") roomId: Long
    ): NetworkResponse<BaseResponse<RoomInfoResponse>, ErrorResponse>

    @GET("/live_user/v1/UserInfo/get_anchor_in_room")
    suspend fun getRoomAnchorInfoAsync(
        @Query("roomid") roomId: Long
    ): NetworkResponse<BaseResponse<RoomAnchorInfoResponse>, ErrorResponse>

    @GET("/xlive/web-room/v2/index/getRoomPlayInfo?protocol=0,1&format=0,1,2&codec=0,1&platform=web&ptype=8")
    suspend fun getFlvLiveStreamUrlAsync(
        @Query("room_id") roomId: Long,
        @Query("qn") videoQuality: Int
    ): NetworkResponse<BaseResponse<LiveStreamUrlResponse>, ErrorResponse>

    @GET("/room/v1/Room/playUrl?platform=h5&otype=json")
    suspend fun getHlsLiveStreamUrlAsync(
        @Query("cid") roomId: Long,
        @Query("quality") videoQuality: Int
    ): NetworkResponse<BaseResponse<String>, ErrorResponse>

    @GET("/xlive/web-room/v1/index/getDanmuInfo")
    suspend fun getDanmakuServerAsync(@Query("id") roomId: Long): NetworkResponse<BaseResponse<DanmakuServerResponse>, ErrorResponse>

    companion object {
        const val BILI_LIVE_HOST = "https://api.live.bilibili.com/"
    }
}