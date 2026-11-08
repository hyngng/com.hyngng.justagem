package com.hyngng.gemtalk

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hyngng.gemtalk.ui.GemTalkApp
import com.hyngng.gemtalk.ui.theme.GemTalkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        requestMaxRefreshRate()
        super.onCreate(savedInstanceState)
        setContent {
            GemTalkTheme {
                GemTalkApp()
            }
        }
    }

    /**
     * Android may stay on the display's default (often 60Hz) mode even on
     * 120Hz panels. Pin the highest-refresh mode at the native resolution so
     * compositing and animations run at the panel's full rate — same intent
     * as Unity's targetFrameRate = the device maximum.
     */
    private fun requestMaxRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return
        val native = display.mode
        val fastest = display.supportedModes
            .filter { it.physicalWidth == native.physicalWidth && it.physicalHeight == native.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        if (fastest.modeId != native.modeId) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = fastest.modeId }
        }
    }
}
