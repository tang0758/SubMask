package com.submask.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class OverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startAsForeground()
        return START_STICKY
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

    companion object {
        private const val CHANNEL_ID = "submask_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.submask.app.action.STOP"

        fun stopIntent(context: Context): Intent {
            return Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
