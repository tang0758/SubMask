package com.submask.app.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class OverlayView(context: Context) : FrameLayout(context) {
    private val lockButton: TextView

    init {
        setBackgroundColor(Color.argb(184, 0, 0, 0))

        lockButton = TextView(context).apply {
            text = "锁"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(80, 255, 255, 255))
        }

        addView(lockButton, LayoutParams(56, 56, Gravity.END or Gravity.TOP))
    }

    fun setMaskOpacity(opacity: Float) {
        val alpha = (opacity.coerceIn(0.20f, 1.00f) * 255).toInt()
        setBackgroundColor(Color.argb(alpha, 0, 0, 0))
    }
}
