package com.submask.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.submask.app.config.OverlayConfigStore
import com.submask.app.overlay.OverlayService
import com.submask.app.ui.MainScreen

class MainActivity : Activity() {
    private lateinit var configStore: OverlayConfigStore
    private lateinit var mainScreen: MainScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configStore = OverlayConfigStore(this)
        mainScreen = MainScreen(
            context = this,
            initialOpacityPercent = (configStore.getOpacity() * 100).toInt(),
            onOpacityChanged = { percent ->
                configStore.setOpacity(percent / 100f)
            },
            onStartClicked = {
                startBlockingOrRequestPermission()
            }
        )
        setContentView(mainScreen)
    }

    override fun onResume() {
        super.onResume()
        mainScreen.setPermissionStatus(Settings.canDrawOverlays(this))
    }

    private fun startBlockingOrRequestPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        startForegroundService(Intent(this, OverlayService::class.java))
    }
}
