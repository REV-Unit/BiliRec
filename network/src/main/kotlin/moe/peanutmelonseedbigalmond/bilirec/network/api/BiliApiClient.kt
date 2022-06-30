package moe.peanutmelonseedbigalmond.bilirec.network.api

import com.haroldadmin.cnradapter.NetworkResponse
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.peanutmelonseedbigalmond.bilirec.network.api.response.DanmakuServerResponse
import moe.peanutmelonseedbigalmond.bilirec.network.api.response.LiveStreamUrlResponse
import moe.peanutmelonseedbigalmond.bilirec.network.api.response.RoomAnchorInfoResponse
import moe.peanutmelonseedbigalmond.bilirec.network.api.response.RoomInfoResponse
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.Duration

class BiliApiClient(private val client: OkHttpClient, biliLiveHost: String) {
    companion object {
        private val DEFAULT_HEADERS = mapOf(
            "accept" to "application/json, text/javascript, */*; q=0.01",
            "origin" to "https://live.bilibili.com",
            "referer" to "https://live.bilibili.com",
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36"
        )
        private val DEFAULT_TIMEOUT_SPAN = Duration.ofSeconds(60)
        private val DEFAULT_OKHTTP_CLIENT = OkHttpClient.Builder()
            .addInterceptor {
                val requestBuilder = it.request().newBuilder()
                val headers = it.request()
                    .headers
                    .newBuilder()
                    .addAll(DEFAULT_HEADERS.toHeaders())
                    .build()
                val newRequest = requestBuilder.headers(headers)
                    .build()
                return@addInterceptor it.proceed(newRequest)
            }
            .readTimeout(DEFAULT_TIMEOUT_SPAN)
            .writeTimeout(DEFAULT_TIMEOUT_SPAN)
            .connectTimeout(DEFAULT_TIMEOUT_SPAN)
            .build()
        val DEFAULT_CLIENT = BiliApiClient(DEFAULT_OKHTTP_CLIENT, BiliApi.BILI_LIVE_HOST)
    }

    private val biliApiImpl = Retrofit.Builder()
        .client(client)
        .addCallAdapterFactory(NetworkResponseAdapterFactory())
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(biliLiveHost)
        .build().create(BiliApi::class.java)

    suspend fun getRoomInfo(roomId: Long): RoomInfoResponse =
        makeSureResponseSuccess(biliApiImpl.getRoomInfoAsync(roomId)).data

    suspend fun getRoomAnchorInfo(roomId: Long): RoomAnchorInfoResponse =
        makeSureResponseSuccess(biliApiImpl.getRoomAnchorInfoAsync(roomId)).data

    suspend fun getFlvLiveStreamUrlInfo(roomId: Long, quality: Int): LiveStreamUrlResponse =
        makeSureResponseSuccess(biliApiImpl.getFlvLiveStreamUrlAsync(roomId, quality)).data

    suspend fun getHlsLiveStreamUrlInfo(roomId: Long, quality: Int): String =
        makeSureResponseSuccess(biliApiImpl.getHlsLiveStreamUrlAsync(roomId, quality)).data

    suspend fun getDanmakuServer(roomId: Long): DanmakuServerResponse =
        makeSureResponseSuccess(biliApiImpl.getDanmakuServerAsync(roomId)).data

    suspend fun getResponseAsync(url: String, timeout: Duration = DEFAULT_TIMEOUT_SPAN): Response {
        val c = client.newBuilder()
            .readTimeout(timeout)
            .writeTimeout(timeout)
            .connectTimeout(timeout)
            .build()

        val req = Request.Builder()
            .url(url)
            .get().build()
        return withContext(Dispatchers.IO){ c.newCall(req).execute() }
    }

    private fun <T : Any> makeSureResponseSuccess(response: NetworkResponse<T, Any>): T {
        when (response) {
            is NetworkResponse.Success -> return response.body
            else -> throw (response as NetworkResponse.Error).error
        }
    }
}