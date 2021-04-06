package kr.co.pflogistics

import android.content.Context
import android.util.Log
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request


/**
 * @todo OKHttp (Http Connection Util)
 */


class HttpUtil() {

    private val client = OkHttpClient()

    fun get(httpUrl: String, callback: Callback) {

        val request = Request.Builder()
            .addHeader("Authorization", "KakaoAK adb6ce5526f7defccdf3fed9f6610074")
            .addHeader("X-NCP-APIGW-API-KEY-ID", "ywgp3sltx8")
            .addHeader("X-NCP-APIGW-API-KEY", "dV0y6BngR29M7r74qe3Tr8D8oyiKLfybhQ0gj9IY")
            .url(httpUrl)
            .build()

        Log.d("httpUrl>>>", httpUrl)

        client.newCall(request).enqueue(callback);
    }

    companion object {

        @Volatile private var instance: HttpUtil? = null

        @JvmStatic fun getInstance(context: Context): HttpUtil =
            instance ?: synchronized(this) {
                instance ?: HttpUtil().also {
                    instance = it
                }
            }

        private val TAG: String? = HttpUtil::class.simpleName;
    }



}