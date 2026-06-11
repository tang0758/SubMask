package com.submask.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.submask.app.config.OverlayConfigStore
import com.submask.app.overlay.OverlayService
import com.submask.app.ui.MainScreen
import com.submask.app.orientation.ScreenBounds

class MainActivity : Activity() {
    private lateinit var configStore: OverlayConfigStore
    private lateinit var mainScreen: MainScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configStore = OverlayConfigStore(this)
        
        val metrics = resources.displayMetrics
        val bounds = ScreenBounds(metrics.widthPixels, metrics.heightPixels)
        
        mainScreen = MainScreen(
            context = this,
            configStore = configStore,
            screenBounds = bounds,
            onOverlayPermissionClicked = {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            },
            onNotificationPermissionClicked = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
                }
            },
            onStartClicked = {
                startBlockingOrRequestPermission()
            },
            onStopClicked = {
                stopService(OverlayService.stopIntent(this))
            }
        )
        setContentView(mainScreen)
    }

    override fun onResume() {
        super.onResume()
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        mainScreen.setPermissionStatus(hasOverlay, hasNotification)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }

        startForegroundService(OverlayService.startIntent(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startForegroundService(OverlayService.startIntent(this))
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 100
    }
}
