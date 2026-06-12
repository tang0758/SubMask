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
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.submask.app.config.MaskRect

class OverlayView(
    context: Context,
    initialRect: MaskRect,
    initialLocked: Boolean,
    initialOpacity: Float,
    private val onRectChanged: (MaskRect) -> Unit,
    private val onLockChanged: (Boolean) -> Unit,
    private val onOpacityChanged: (Float) -> Unit,
    private val onCloseRequested: () -> Unit
) : FrameLayout(context) {
    private val lockButton: ImageView
    private val closeButton: ImageView
    private val opacityButton: TextView
    private val opacityPanel: LinearLayout
    private val opacityValueText: TextView
    private val opacitySeekBar: SeekBar
    private val resizeHandle: TextView
    private var rect = initialRect
    private var locked = initialLocked
    private var opacityPercent = OverlayOpacityControl.percentFromOpacity(initialOpacity)
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var startRect = initialRect
    private var mode = TouchMode.NONE
    private val controlSize = CONTROL_SIZE_PX

    init {
        clipChildren = false
        clipToPadding = false

        lockButton = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.argb(90, 255, 255, 255))
            setOnClickListener {
                setLocked(!locked)
            }
        }
        addView(lockButton, LayoutParams(controlSize, controlSize, Gravity.END or Gravity.TOP))

        closeButton = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.argb(90, 255, 255, 255))
            setImageDrawable(CloseDrawable())
            setOnClickListener {
                onCloseRequested()
            }
        }
        addView(closeButton, LayoutParams(controlSize, controlSize, Gravity.START or Gravity.TOP))

        opacityButton = TextView(context).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setBackgroundColor(Color.argb(110, 255, 255, 255))
            setOnClickListener {
                opacityPanel.visibility = if (opacityPanel.visibility == VISIBLE) GONE else VISIBLE
            }
        }
        addView(opacityButton, LayoutParams(controlSize, controlSize, Gravity.START or Gravity.BOTTOM))

        opacityValueText = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        opacitySeekBar = SeekBar(context).apply {
            max = OverlayOpacityControl.MAX_PERCENT - OverlayOpacityControl.MIN_PERCENT
            progress = opacityPercent - OverlayOpacityControl.MIN_PERCENT
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val percent = OverlayOpacityControl.MIN_PERCENT + progress
                    val opacity = OverlayOpacityControl.opacityFromPercent(percent)
                    setMaskOpacity(opacity)
                    onOpacityChanged(opacity)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        opacityPanel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 0, 16, 0)
            setBackgroundColor(Color.argb(130, 255, 255, 255))
            visibility = GONE
            addView(opacityValueText, LinearLayout.LayoutParams(OPACITY_VALUE_WIDTH_PX, LinearLayout.LayoutParams.MATCH_PARENT))
            addView(opacitySeekBar, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(opacityPanel, LayoutParams(LayoutParams.MATCH_PARENT, controlSize, Gravity.BOTTOM).apply {
            leftMargin = controlSize
            rightMargin = controlSize
        })

        resizeHandle = TextView(context).apply {
            text = "↘"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(90, 255, 255, 255))
        }
        addView(resizeHandle, LayoutParams(controlSize, controlSize, Gravity.END or Gravity.BOTTOM))

        setMaskOpacity(initialOpacity)
        setLocked(initialLocked, notifyChange = false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateControlLayoutForHeight(h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (locked) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                dragStartY = event.rawY
                startRect = rect
                mode = if (event.x > width - controlSize && event.y > height - controlSize) {
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
        opacityPercent = OverlayOpacityControl.percentFromOpacity(opacity)
        opacityButton.text = "$opacityPercent%"
        opacityValueText.text = "$opacityPercent%"
        opacitySeekBar.progress = opacityPercent - OverlayOpacityControl.MIN_PERCENT
        val alpha = (OverlayOpacityControl.opacityFromPercent(opacityPercent) * 255).toInt()
        setBackgroundColor(Color.argb(alpha, 0, 0, 0))
    }

    fun setRect(newRect: MaskRect) {
        rect = newRect
    }

    fun setLocked(newLocked: Boolean, notifyChange: Boolean = true) {
        locked = newLocked
        lockButton.setImageDrawable(LockDrawable(locked))
        closeButton.visibility = if (locked) GONE else VISIBLE
        opacityButton.visibility = if (locked) GONE else VISIBLE
        if (locked) {
            opacityPanel.visibility = GONE
        }
        resizeHandle.visibility = if (locked) GONE else VISIBLE
        if (notifyChange) {
            onLockChanged(locked)
        }
    }

    private fun updateControlLayoutForHeight(height: Int) {
        val compact = height < controlSize * 2
        val bottomOffset = if (compact) height - controlSize * 2 else 0
        listOf(opacityButton, resizeHandle).forEach { view ->
            val params = view.layoutParams as LayoutParams
            params.bottomMargin = bottomOffset
            view.layoutParams = params
        }
        val panelParams = opacityPanel.layoutParams as LayoutParams
        panelParams.bottomMargin = bottomOffset
        opacityPanel.layoutParams = panelParams
    }

    companion object {
        private const val CONTROL_SIZE_PX = 56
        private const val OPACITY_PANEL_WIDTH_PX = 220
        private const val OPACITY_VALUE_WIDTH_PX = 64
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

class CloseDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val size = Math.min(w, h) * 0.4f
        
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(size / 24f, size / 24f)
        
        canvas.drawLine(-6f, -6f, 6f, 6f, paint)
        canvas.drawLine(6f, -6f, -6f, 6f, paint)
        
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
