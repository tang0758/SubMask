package com.submask.app.config

import com.submask.app.orientation.ScreenBounds
import kotlin.math.max
import kotlin.math.min

data class MaskRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun clampTo(bounds: ScreenBounds, minWidth: Int, minHeight: Int): MaskRect {
        val clampedWidth = min(max(width, minWidth), bounds.width)
        val clampedHeight = min(max(height, minHeight), bounds.height)
        val maxX = max(0, bounds.width - clampedWidth)
        val maxY = max(0, bounds.height - clampedHeight)
        return copy(
            x = x.coerceIn(0, maxX),
            y = y.coerceIn(0, maxY),
            width = clampedWidth,
            height = clampedHeight
        )
    }
}

data class OrientationConfig(
    val rect: MaskRect,
    val locked: Boolean,
    val initialized: Boolean
)

data class OverlayConfig(
    val opacity: Float,
    val portrait: OrientationConfig,
    val landscape: OrientationConfig
)
