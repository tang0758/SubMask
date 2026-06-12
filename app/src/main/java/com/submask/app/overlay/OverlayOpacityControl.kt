package com.submask.app.overlay

import kotlin.math.roundToInt

object OverlayOpacityControl {
    const val MIN_PERCENT = 20
    const val MAX_PERCENT = 100

    fun opacityFromPercent(percent: Int): Float {
        return percent.coerceIn(MIN_PERCENT, MAX_PERCENT) / 100f
    }

    fun percentFromOpacity(opacity: Float): Int {
        return (opacity.coerceIn(0.20f, 1.00f) * 100).roundToInt()
    }
}
