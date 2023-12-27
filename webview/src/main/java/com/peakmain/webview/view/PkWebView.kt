package com.peakmain.webview.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleRegistry
import com.peakmain.webview.helper.WebViewHelper
import com.peakmain.webview.manager.WebViewController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * author ：Peakmain
 * createTime：2023/4/1
 * mail:2726449200@qq.com
 * describe：
 */
class PkWebView : WebView {
    var blackMonitorCallback: ((Boolean) -> Unit)? = null


    fun postBlankMonitorRunnable() {
        removeCallbacks(blackMonitorRunnable)
        postDelayed(blackMonitorRunnable, 1000)
    }

    fun removeBlankMonitorRunnable() {
        removeCallbacks(blackMonitorRunnable)
    }

    private val blackMonitorRunnable = Runnable {
        val height = measuredHeight / 6
        val width = measuredWidth / 6
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).run {
            draw(this)
        }
        with(bitmap) {
            //像素总数
            val pixelCount = (width * height).toFloat()
            val whitePixelCount = getWhitePixelCount()
            recycle()
            if (whitePixelCount == 0) return@Runnable
            post {
                blackMonitorCallback?.invoke(whitePixelCount / pixelCount > 0.95)
            }
        }

    }

    private fun Bitmap.getWhitePixelCount(): Int {
        var whitePixelCount = 0
        for (i in 0 until width) {
            for (j in 0 until height) {
                if (getPixel(i, j) == -1) {
                    whitePixelCount++
                }
            }
        }
        return whitePixelCount
    }

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        WebViewHelper.loadWebViewResource(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        WebViewHelper.loadWebViewResource(context)
    }

    constructor(
        context: Context,
    ) : this(context, null)


    private var mParams: WebViewController.WebViewParams? = null
    var mLoadUrlListener: ((String) -> String)? = null

    override fun loadUrl(url: String) {
        var newUrl = url
        if (mLoadUrlListener != null) {
            newUrl = mLoadUrlListener!!.invoke(url)
        }
        super.loadUrl(newUrl)
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        mLoadUrlListener?.invoke(url)
        super.loadUrl(url, additionalHttpHeaders)
    }

    fun setWebViewParams(params: WebViewController.WebViewParams) {
        this.mParams = params
    }

    fun getWebViewParams(): WebViewController.WebViewParams? {
        return mParams
    }


}