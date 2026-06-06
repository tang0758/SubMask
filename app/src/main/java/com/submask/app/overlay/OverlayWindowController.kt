package com.submask.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.submask.app.config.MaskRect

class OverlayWindowController(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var view: View? = null

    fun show(overlayView: OverlayView, rect: MaskRect) {
        if (view != null) return
        view = overlayView
        windowManager.addView(overlayView, layoutParams(rect))
    }

    fun update(rect: MaskRect) {
        val currentView = view ?: return
        windowManager.updateViewLayout(currentView, layoutParams(rect))
    }

    fun remove() {
        val currentView = view ?: return
        windowManager.removeView(currentView)
        view = null
    }

    private fun layoutParams(rect: MaskRect): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            rect.width,
            rect.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = rect.x
            y = rect.y
        }
    }
}
