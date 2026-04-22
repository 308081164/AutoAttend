package com.liubang.liubang_shell

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
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
    }
}
