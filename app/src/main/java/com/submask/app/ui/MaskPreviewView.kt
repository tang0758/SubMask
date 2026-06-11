package com.submask.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.submask.app.config.MaskRect
import com.submask.app.config.OverlayConfigStore
import com.submask.app.orientation.ScreenBounds

class MaskPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maskRect: MaskRect? = null
    private var screenBounds = ScreenBounds(1080, 1920)
    private var isLandscape = false
    private var opacity = 0.72f
    
    // Callback when user drags or resizes the mask directly
    var onRectUpdated: ((MaskRect) -> Unit)? = null

    // Touch variables
    private var touchMode = HandleType.NONE
    private var startTouchX = 0f
    private var startTouchY = 0f
    private var startRect = MaskRect(0, 0, 100, 100)

    private val phoneBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val maskBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val maskBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(95, 99, 104)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun update(rect: MaskRect, bounds: ScreenBounds, landscape: Boolean, currentOpacity: Float) {
        maskRect = rect
        screenBounds = bounds
        isLandscape = landscape
        opacity = currentOpacity
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Phone aspect ratio depending on orientation tab
        val ratio = if (isLandscape) 16f / 9f else 9f / 16f
        
        val padding = 8f
        val maxW = w - padding * 2
        val maxH = h - padding * 2
        
        val boxW: Float
        val boxH: Float
        if (maxW / maxH > ratio) {
            boxH = maxH
            boxW = boxH * ratio
        } else {
            boxW = maxW
            boxH = boxW / ratio
        }
        
        val left = (w - boxW) / 2f
        val top = (h - boxH) / 2f
        val right = left + boxW
        val bottom = top + boxH

        // 1. Draw beautiful landscape background inside the phone screen
        canvas.save()
        val screenPath = Path().apply {
            addRoundRect(RectF(left, top, right, bottom), 20f, 20f, Path.Direction.CW)
        }
        canvas.clipPath(screenPath)

        // Sky gradient: blue to orange-ish sunset
        val skyShader = LinearGradient(
            left, top, left, bottom,
            intArrayOf(Color.rgb(135, 188, 222), Color.rgb(222, 235, 245), Color.rgb(240, 220, 200)),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = skyShader
        }
        canvas.drawRect(left, top, right, bottom, bgPaint)

        // Draw mountains (Mount Fuji style)
        val fujiW = boxW * 0.7f
        val fujiH = boxH * 0.35f
        val fujiCx = left + boxW * 0.5f
        val fujiCy = top + boxH * 0.65f

        val fujiPath = Path().apply {
            moveTo(fujiCx - fujiW * 0.6f, bottom)
            quadTo(fujiCx - fujiW * 0.2f, fujiCy - fujiH * 0.1f, fujiCx - fujiW * 0.08f, fujiCy - fujiH)
            lineTo(fujiCx + fujiW * 0.08f, fujiCy - fujiH)
            quadTo(fujiCx + fujiW * 0.2f, fujiCy - fujiH * 0.1f, fujiCx + fujiW * 0.6f, bottom)
            close()
        }
        mountainPaint.color = Color.rgb(52, 97, 122)
        canvas.drawPath(fujiPath, mountainPaint)

        // Draw snow cap on Mount Fuji
        val snowPath = Path().apply {
            moveTo(fujiCx - fujiW * 0.15f, fujiCy - fujiH * 0.6f)
            lineTo(fujiCx - fujiW * 0.08f, fujiCy - fujiH)
            lineTo(fujiCx + fujiW * 0.08f, fujiCy - fujiH)
            lineTo(fujiCx + fujiW * 0.15f, fujiCy - fujiH * 0.6f)
            quadTo(fujiCx, fujiCy - fujiH * 0.45f, fujiCx - fujiW * 0.15f, fujiCy - fujiH * 0.6f)
            close()
        }
        val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(snowPath, snowPaint)

        // Draw smaller green hills
        val hillPath1 = Path().apply {
            moveTo(left, bottom)
            quadTo(left + boxW * 0.25f, bottom - boxH * 0.15f, left + boxW * 0.5f, bottom)
            close()
        }
        mountainPaint.color = Color.rgb(44, 115, 96)
        canvas.drawPath(hillPath1, mountainPaint)

        val hillPath2 = Path().apply {
            moveTo(left + boxW * 0.4f, bottom)
            quadTo(left + boxW * 0.75f, bottom - boxH * 0.18f, right, bottom)
            close()
        }
        mountainPaint.color = Color.rgb(28, 92, 75)
        canvas.drawPath(hillPath2, mountainPaint)

        // Draw water lake overlay at the very bottom
        val waterPath = Path().apply {
            addRect(left, bottom - boxH * 0.08f, right, bottom, Path.Direction.CW)
        }
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 62, 85)
            style = Paint.Style.FILL
        }
        canvas.drawPath(waterPath, waterPaint)

        // 2. Draw mask rectangle inside it
        val rect = maskRect ?: return
        
        // Sim bounds based on target preview orientation
        val simW = if (isLandscape) {
            Math.max(screenBounds.width, screenBounds.height).toFloat()
        } else {
            Math.min(screenBounds.width, screenBounds.height).toFloat()
        }
        val simH = if (isLandscape) {
            Math.min(screenBounds.width, screenBounds.height).toFloat()
        } else {
            Math.max(screenBounds.width, screenBounds.height).toFloat()
        }

        val scaleX = boxW / simW
        val scaleY = boxH / simH

        val maskLeft = left + rect.x * scaleX
        val maskTop = top + rect.y * scaleY
        val maskRight = maskLeft + rect.width * scaleX
        val maskBottom = maskTop + rect.height * scaleY

        // Draw semi-transparent black background
        maskBgPaint.alpha = (opacity.coerceIn(0.20f, 1.00f) * 255).toInt()
        val maskRectF = RectF(maskLeft, maskTop, maskRight, maskBottom)
        canvas.drawRect(maskRectF, maskBgPaint)

        // Draw dotted white border
        canvas.drawRect(maskRectF, maskBorderPaint)

        // Draw text: "这是示例字幕内容"
        val sampleText = "这是示例字幕内容"
        textPaint.textSize = Math.min(boxW * 0.06f, boxH * 0.06f).coerceIn(12f, 26f)
        
        // Vertically center text in the mask
        val boundsRect = Rect()
        textPaint.getTextBounds(sampleText, 0, sampleText.length, boundsRect)
        val textHeight = boundsRect.height()
        val textY = maskTop + (maskBottom - maskTop) / 2f + textHeight / 2f
        canvas.drawText(sampleText, maskLeft + (maskRight - maskLeft) / 2f, textY, textPaint)

        // 3. Draw circular white handles
        val handleRadius = dpToPx(6)
        
        val hTopLeft = Pair(maskLeft, maskTop)
        val hTopRight = Pair(maskRight, maskTop)
        val hBottomLeft = Pair(maskLeft, maskBottom)
        val hBottomRight = Pair(maskRight, maskBottom)
        val hTopCenter = Pair((maskLeft + maskRight) / 2f, maskTop)
        val hBottomCenter = Pair((maskLeft + maskRight) / 2f, maskBottom)

        val handles = listOf(hTopLeft, hTopRight, hBottomLeft, hBottomRight, hTopCenter, hBottomCenter)
        for (h in handles) {
            canvas.drawCircle(h.first, h.second, handleRadius, handlePaint)
            canvas.drawCircle(h.first, h.second, handleRadius, handleBorderPaint)
        }

        canvas.restore()

        // 4. Draw phone outline and bezel
        val borderRectF = RectF(left, top, right, bottom)
        canvas.drawRoundRect(borderRectF, 20f, 20f, phoneBorderPaint)

        // Draw punch hole camera
        val cameraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        if (isLandscape) {
            canvas.drawCircle(left + dpToPx(8), top + boxH / 2f, dpToPx(4), cameraPaint)
        } else {
            canvas.drawCircle(left + boxW / 2f, top + dpToPx(8), dpToPx(4), cameraPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rect = maskRect ?: return super.onTouchEvent(event)
        
        // Retrieve preview dimensions
        val w = width.toFloat()
        val h = height.toFloat()
        val ratio = if (isLandscape) 16f / 9f else 9f / 16f
        val padding = 8f
        val maxW = w - padding * 2
        val maxH = h - padding * 2
        
        val boxW: Float
        val boxH: Float
        if (maxW / maxH > ratio) {
            boxH = maxH
            boxW = boxH * ratio
        } else {
            boxW = maxW
            boxH = boxW / ratio
        }
        
        val left = (w - boxW) / 2f
        val top = (h - boxH) / 2f

        // Retrieve scaling bounds
        val simW = if (isLandscape) {
            Math.max(screenBounds.width, screenBounds.height).toFloat()
        } else {
            Math.min(screenBounds.width, screenBounds.height).toFloat()
        }
        val simH = if (isLandscape) {
            Math.min(screenBounds.width, screenBounds.height).toFloat()
        } else {
            Math.max(screenBounds.width, screenBounds.height).toFloat()
        }

        val scaleX = boxW / simW
        val scaleY = boxH / simH

        val maskLeft = left + rect.x * scaleX
        val maskTop = top + rect.y * scaleY
        val maskRight = maskLeft + rect.width * scaleX
        val maskBottom = maskTop + rect.height * scaleY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startTouchX = event.x
                startTouchY = event.y
                startRect = rect
                
                val touchRadius = dpToPx(24) // hit range
                
                val hTopLeft = Pair(maskLeft, maskTop)
                val hTopRight = Pair(maskRight, maskTop)
                val hBottomLeft = Pair(maskLeft, maskBottom)
                val hBottomRight = Pair(maskRight, maskBottom)
                val hTopCenter = Pair((maskLeft + maskRight) / 2f, maskTop)
                val hBottomCenter = Pair((maskLeft + maskRight) / 2f, maskBottom)

                touchMode = when {
                    dist(event.x, event.y, hTopLeft) < touchRadius -> HandleType.TOP_LEFT
                    dist(event.x, event.y, hTopRight) < touchRadius -> HandleType.TOP_RIGHT
                    dist(event.x, event.y, hBottomLeft) < touchRadius -> HandleType.BOTTOM_LEFT
                    dist(event.x, event.y, hBottomRight) < touchRadius -> HandleType.BOTTOM_RIGHT
                    dist(event.x, event.y, hTopCenter) < touchRadius -> HandleType.TOP_CENTER
                    dist(event.x, event.y, hBottomCenter) < touchRadius -> HandleType.BOTTOM_CENTER
                    event.x in maskLeft..maskRight && event.y in maskTop..maskBottom -> HandleType.MASK_BODY
                    else -> HandleType.NONE
                }
                
                if (touchMode != HandleType.NONE) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode == HandleType.NONE) return false

                val dx = ((event.x - startTouchX) / scaleX).toInt()
                val dy = ((event.y - startTouchY) / scaleY).toInt()

                var newX = startRect.x
                var newY = startRect.y
                var newW = startRect.width
                var newH = startRect.height

                when (touchMode) {
                    HandleType.MASK_BODY -> {
                        newX = startRect.x + dx
                        newY = startRect.y + dy
                    }
                    HandleType.TOP_LEFT -> {
                        newX = startRect.x + dx
                        newY = startRect.y + dy
                        newW = startRect.width - dx
                        newH = startRect.height - dy
                    }
                    HandleType.TOP_RIGHT -> {
                        newY = startRect.y + dy
                        newW = startRect.width + dx
                        newH = startRect.height - dy
                    }
                    HandleType.BOTTOM_LEFT -> {
                        newX = startRect.x + dx
                        newW = startRect.width - dx
                        newH = startRect.height + dy
                    }
                    HandleType.BOTTOM_RIGHT -> {
                        newW = startRect.width + dx
                        newH = startRect.height + dy
                    }
                    HandleType.TOP_CENTER -> {
                        newY = startRect.y + dy
                        newH = startRect.height - dy
                    }
                    HandleType.BOTTOM_CENTER -> {
                        newH = startRect.height + dy
                    }
                    else -> {}
                }

                // Protect width and height from going negative or below minimum
                val minW = OverlayConfigStore.MIN_MASK_WIDTH
                val minH = OverlayConfigStore.MIN_MASK_HEIGHT

                if (newW < minW) {
                    if (touchMode == HandleType.TOP_LEFT || touchMode == HandleType.BOTTOM_LEFT) {
                        newX -= (minW - newW)
                    }
                    newW = minW
                }
                if (newH < minH) {
                    if (touchMode == HandleType.TOP_LEFT || touchMode == HandleType.TOP_RIGHT || touchMode == HandleType.TOP_CENTER) {
                        newY -= (minH - newH)
                    }
                    newH = minH
                }

                val rawRect = MaskRect(x = newX, y = newY, width = newW, height = newH)
                val clamped = rawRect.clampTo(
                    bounds = ScreenBounds(simW.toInt(), simH.toInt()),
                    minWidth = minW,
                    minHeight = minH
                )

                maskRect = clamped
                onRectUpdated?.invoke(clamped)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                touchMode = HandleType.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dist(x: Float, y: Float, p: Pair<Float, Float>): Float {
        val dx = x - p.first
        val dy = y - p.second
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun dpToPx(dp: Int): Float {
        return dp * context.resources.displayMetrics.density
    }

    private enum class HandleType {
        NONE,
        MASK_BODY,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_CENTER,
        BOTTOM_CENTER
    }
}
