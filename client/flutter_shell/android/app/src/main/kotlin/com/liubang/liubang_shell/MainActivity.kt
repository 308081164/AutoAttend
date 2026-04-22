package com.liubang.liubang_shell

import android.content.pm.ActivityInfo
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 平板检测：短边 >= 600dp 则强制横屏（支持180度旋转）
        enforceLandscapeForTablet()

        // 延迟查找 WebView 并注入 SSL 错误处理 + 缓存配置
        Handler(Looper.getMainLooper()).postDelayed({
            patchWebView()
        }, 1000)
    }

    private fun enforceLandscapeForTablet() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val realMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(realMetrics)
        val density = realMetrics.density
        val shortestDp = Math.min(realMetrics.widthPixels, realMetrics.heightPixels) / density
        if (shortestDp >= 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun patchWebView() {
        val rootView = window.decorView.rootView ?: return
        walkAndPatch(rootView)
        // WebView 可能在 Flutter 渲染后才创建，再延迟一次
        Handler(Looper.getMainLooper()).postDelayed({
            walkAndPatch(rootView)
        }, 3000)
    }

    private fun walkAndPatch(view: View) {
        if (view is WebView) {
            // 修复缓存模式：使用 LOAD_DEFAULT 避免缓存未命中
            view.settings.cacheMode = WebSettings.LOAD_DEFAULT
            view.settings.domStorageEnabled = true
            view.settings.databaseEnabled = true
            view.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // 保留原始 WebViewClient，仅注入 SSL 错误处理
            val original = view.webViewClient
            view.webViewClient = object : WebViewClient() {
                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    handler?.proceed()
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: String?): Boolean {
                    return original?.shouldOverrideUrlLoading(view, request) ?: false
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                walkAndPatch(view.getChildAt(i))
            }
        }
    }
}
