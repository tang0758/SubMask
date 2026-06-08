package com.submask.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.submask.app.config.MaskRect

class OverlayView(
    context: Context,
    initialRect: MaskRect,
    initialLocked: Boolean,
    private val onRectChanged: (MaskRect) -> Unit,
    private val onLockChanged: (Boolean) -> Unit
) : FrameLayout(context) {
    private val lockButton: ImageView
    private val resizeHandle: TextView
    private var rect = initialRect
    private var locked = initialLocked
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var startRect = initialRect
    private var mode = TouchMode.NONE

    init {
        setMaskOpacity(0.72f)

        lockButton = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
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
        lockButton.setImageDrawable(LockDrawable(locked))
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

class LockDrawable(private val locked: Boolean) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val keyholePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val size = Math.min(w, h) * 0.45f
        
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(size / 24f, size / 24f)
        
        // 1. Lock Body
        paint.style = Paint.Style.FILL
        val bodyRect = RectF(-7f, -1f, 7f, 9f)
        canvas.drawRoundRect(bodyRect, 1.5f, 1.5f, paint)
        
        // 2. Lock Shackle (loop)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        
        if (locked) {
            val shackleRect = RectF(-4f, -7f, 4f, -1f)
            canvas.drawArc(shackleRect, 180f, 180f, false, paint)
            canvas.drawLine(-4f, -1f, -4f, -4f, paint)
            canvas.drawLine(4f, -1f, 4f, -4f, paint)
        } else {
            val shackleRect = RectF(-4f, -9f, 4f, -3f)
            canvas.drawArc(shackleRect, 180f, 180f, false, paint)
            canvas.drawLine(-4f, -3f, -4f, -6f, paint)
        }
        
        // 3. Keyhole
        keyholePaint.style = Paint.Style.FILL
        canvas.drawCircle(0f, 2.5f, 1f, keyholePaint)
        keyholePaint.style = Paint.Style.STROKE
        keyholePaint.strokeWidth = 1.5f
        canvas.drawLine(0f, 2.5f, 0f, 5.5f, keyholePaint)
        
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        keyholePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        keyholePaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
