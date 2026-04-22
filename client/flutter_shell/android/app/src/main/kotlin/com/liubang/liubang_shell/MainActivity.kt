package com.liubang.liubang_shell

import android.content.pm.ActivityInfo
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import io.flutter.embedding.android.FlutterActivity

class MainActivity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 平板检测：短边 >= 600dp 则强制横屏（支持180度旋转）
        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        val widthPx = display.width
        val heightPx = display.height
        val density = resources.displayMetrics.density
        val shortestDp = Math.min(widthPx, heightPx) / density
        if (shortestDp >= 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        // 延迟查找 WebView 并设置 SSL 错误处理（忽略证书错误）
        Handler(Looper.getMainLooper()).postDelayed({
            setupWebViewSslHandler()
        }, 500)
    }

    private fun setupWebViewSslHandler() {
        val rootView = window.decorView.rootView
        findAndSetupWebViews(rootView)
        // 持续监听，因为 WebView 可能在之后才创建
        Handler(Looper.getMainLooper()).postDelayed({
            findAndSetupWebViews(rootView)
        }, 2000)
    }

    private fun findAndSetupWebViews(view: View) {
        if (view is WebView) {
            view.webViewClient = object : WebViewClient() {
                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed() // 忽略 SSL 证书错误
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAndSetupWebViews(view.getChildAt(i))
            }
        }
    }
}
