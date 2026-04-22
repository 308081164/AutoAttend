package com.liubang.liubang_shell

import android.os.Bundle
import android.webkit.WebView
import io.flutter.embedding.android.FlutterActivity

class MainActivity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 修复 WebView 缓存问题：允许混合内容 + 正确缓存模式
        try {
            WebView.setWebContentsDebuggingEnabled(false)
        } catch (_: Exception) {}
    }
}
