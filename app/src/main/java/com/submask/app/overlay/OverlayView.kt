package com.submask.app.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import com.submask.app.config.MaskRect

class OverlayView(
    context: Context,
    initialRect: MaskRect,
    initialLocked: Boolean,
    private val onRectChanged: (MaskRect) -> Unit,
    private val onLockChanged: (Boolean) -> Unit
) : FrameLayout(context) {
    private val lockButton: TextView
    private val resizeHandle: TextView
    private var rect = initialRect
    private var locked = initialLocked
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var startRect = initialRect
    private var mode = TouchMode.NONE

    init {
        setMaskOpacity(0.72f)

        lockButton = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(90, 255, 255, 255))
            setOnClickListener {
                setLocked(!locked)
            }
        }
        addView(lockButton, LayoutParams(56, 56, Gravity.END or Gravity.TOP))

        resizeHandle = TextView(context).apply {
            text = "↘"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(90, 255, 255, 255))
        }
        addView(resizeHandle, LayoutParams(56, 56, Gravity.END or Gravity.BOTTOM))

        setLocked(initialLocked, notifyChange = false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (locked) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                dragStartY = event.rawY
                startRect = rect
                mode = if (event.x > width - 96 && event.y > height - 96) {
                    TouchMode.RESIZE
                } else {
                    TouchMode.DRAG
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - dragStartX).toInt()
                val dy = (event.rawY - dragStartY).toInt()
                rect = when (mode) {
                    TouchMode.DRAG -> startRect.copy(x = startRect.x + dx, y = startRect.y + dy)
                    TouchMode.RESIZE -> startRect.copy(
                        width = startRect.width + dx,
                        height = startRect.height + dy
                    )
                    TouchMode.NONE -> rect
                }
                onRectChanged(rect)
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                mode = TouchMode.NONE
                return true
            }
        }

        return true
    }

    fun setMaskOpacity(opacity: Float) {
        val alpha = (opacity.coerceIn(0.20f, 1.00f) * 255).toInt()
        setBackgroundColor(Color.argb(alpha, 0, 0, 0))
    }

    fun setRect(newRect: MaskRect) {
        rect = newRect
    }

    fun setLocked(newLocked: Boolean, notifyChange: Boolean = true) {
        locked = newLocked
        lockButton.text = if (locked) "解" else "锁"
        resizeHandle.visibility = if (locked) GONE else VISIBLE
        if (notifyChange) {
            onLockChanged(locked)
        }
    }

    private enum class TouchMode {
        NONE,
        DRAG,
        RESIZE
    }
}
