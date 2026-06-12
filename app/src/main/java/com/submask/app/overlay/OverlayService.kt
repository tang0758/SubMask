package com.submask.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import com.submask.app.config.MaskRect
import com.submask.app.config.OrientationConfig
import com.submask.app.config.OverlayConfigStore
import com.submask.app.orientation.OrientationResolver
import com.submask.app.orientation.OrientationStateSelector
import com.submask.app.orientation.ScreenBounds
import android.content.SharedPreferences
import com.submask.app.config.OverlayConfigKeys
import com.submask.app.orientation.ScreenOrientation
import com.submask.app.tile.SubMaskTileService

class OverlayService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var configStore: OverlayConfigStore
    private lateinit var windowController: OverlayWindowController
    private var overlayView: OverlayView? = null
    private var currentOrientation: ScreenOrientation? = null
    private var currentRect: MaskRect? = null
    private var currentLocked: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        configStore = OverlayConfigStore(this)
        configStore.registerListener(this)
        windowController = OverlayWindowController(this)
        SubMaskTileService.requestRefresh(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startAsForeground()
        showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        windowController.remove()
        overlayView = null
        isRunning = false
        configStore.unregisterListener(this)
        SubMaskTileService.requestRefresh(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == OverlayConfigKeys.opacity) {
            overlayView?.setMaskOpacity(configStore.getOpacity())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyCurrentOrientation()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SubMask",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "字幕遮挡器运行状态"
        }
        manager.createNotificationChannel(channel)
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = stopIntent(this)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("SubMask 正在运行")
            .setContentText("正在显示字幕遮挡条")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭遮挡", stopPendingIntent)
            .build()
    }

    private fun showOverlay() {
        if (overlayView != null) return
        val bounds = currentScreenBounds()
        val orientation = OrientationResolver.resolve(bounds)
        val config = configStore.loadOrientationConfig(orientation, bounds)
        currentOrientation = orientation
        currentRect = config.rect
        currentLocked = config.locked

        val view = OverlayView(
            context = this,
            initialRect = config.rect,
            initialLocked = config.locked,
            initialOpacity = configStore.getOpacity(),
            onRectChanged = { rect ->
                val clamped = rect.clampTo(
                    bounds = currentScreenBounds(),
                    minWidth = OverlayConfigStore.MIN_MASK_WIDTH,
                    minHeight = OverlayConfigStore.MIN_MASK_HEIGHT
                )
                currentRect = clamped
                overlayView?.setRect(clamped)
                windowController.update(clamped)
                saveCurrentConfig()
            },
            onLockChanged = { locked ->
                currentLocked = locked
                saveCurrentConfig()
            },
            onOpacityChanged = { opacity ->
                configStore.setOpacity(opacity)
            },
            onCloseRequested = {
                stopSelf()
            }
        )

        overlayView = view
        windowController.show(view, config.rect)
    }

    private fun saveCurrentConfig() {
        val orientation = currentOrientation ?: return
        val rect = currentRect ?: return
        configStore.saveOrientationConfig(
            orientation,
            OrientationConfig(rect = rect, locked = currentLocked, initialized = true)
        )
    }

    private fun applyCurrentOrientation() {
        val view = overlayView ?: return
        val bounds = currentScreenBounds()
        val newOrientation = OrientationResolver.resolve(bounds)
        if (newOrientation == currentOrientation) return

        saveCurrentConfig()

        val stored = configStore.loadOrientationConfig(newOrientation, bounds)
        val selected = OrientationStateSelector.select(newOrientation, bounds, stored)
        currentOrientation = newOrientation
        currentRect = selected.rect
        currentLocked = selected.locked
        view.setRect(selected.rect)
        view.setLocked(selected.locked, notifyChange = false)
        windowController.update(selected.rect)
    }

    private fun currentScreenBounds(): ScreenBounds {
        val metrics = resources.displayMetrics
        return ScreenBounds(metrics.widthPixels, metrics.heightPixels)
    }

    companion object {
        private const val CHANNEL_ID = "submask_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.submask.app.action.STOP"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startIntent(context: Context): Intent {
            return Intent(context, OverlayService::class.java)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
