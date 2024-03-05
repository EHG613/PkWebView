package com.peakmain.webview.manager

import android.app.Application
import android.content.Context
import android.os.Build
import com.peakmain.webview.bean.cache.CacheRequest
import com.peakmain.webview.bean.cache.WebResource
import com.peakmain.webview.utils.WebViewUtils
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.net.HttpURLConnection
import java.util.Locale

/**
 * author ：Peakmain
 * createTime：2023/11/17
 * mail:2726449200@qq.com
 * describe：
 */
internal class OKHttpManager(context: Context) {
    private val webViewResourceCacheDir by lazy {
        File(context.cacheDir, "PkWebView_okHttp_cache")
    }
    private val USER_AGENT = "User-Agent"

    fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().cache(Cache(webViewResourceCacheDir, 500L * 1024 * 1024))
            .followRedirects(false)
            .followSslRedirects(false)
            .addNetworkInterceptor(getWebViewCacheInterceptor())
            .build()
    }

    private fun getWebViewCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response.newBuilder().removeHeader("pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "max-age=" + (360L * 24 * 60 * 60))
                .build()
        }
    }

    fun getResource(request: CacheRequest, isContentType: Boolean): WebResource? {
        val url = request.url
        val acceptLanguage =
            Locale.getDefault().toLanguageTag()
        val builder = Request.Builder()
            .removeHeader(USER_AGENT)
            .addHeader(USER_AGENT, request.userAgent)
            .addHeader("Accept", "*/*")
            .addHeader("Accept-Language", acceptLanguage)

        val headers = request.headers
        if (headers != null && headers.isNotEmpty()) {
            for ((header, value) in headers) {
                if (!isNeedStripHeader(header)) {
                    builder.removeHeader(header)
                    builder.addHeader(header, value)
                }
            }
        }
        val request = builder.url(url!!)
            .get().build()
        var response: Response? = null

        val remoteResource = WebResource()
        response = createOkHttpClient().newCall(request).execute()
        if (isInterceptorThisRequest(response)) {
            remoteResource.responseCode = response.code
            remoteResource.message = response.message
            remoteResource.isModified = response.code != HttpURLConnection.HTTP_NOT_MODIFIED
            val responseBody: ResponseBody? = response.body
            if (responseBody != null) {
                remoteResource.originBytes = responseBody.bytes()
            }
            remoteResource.responseHeaders = WebViewUtils.instance.generateHeadersMap(response.headers)
            return remoteResource
        }
        return null
    }

    private fun isInterceptorThisRequest(response: Response): Boolean {
        var code = response.code
        return !(code < 100 || code > 599 || (code in 300..399))
    }

    private fun isNeedStripHeader(headerName: String): Boolean {
        return (headerName.equals("If-Match", ignoreCase = true)
                || headerName.equals("If-None-Match", ignoreCase = true)
                || headerName.equals("If-Modified-Since", ignoreCase = true)
                || headerName.equals("If-Unmodified-Since", ignoreCase = true)
                || headerName.equals("Last-Modified", ignoreCase = true)
                || headerName.equals("Expires", ignoreCase = true)
                || headerName.equals("Cache-Control", ignoreCase = true))
    }

}