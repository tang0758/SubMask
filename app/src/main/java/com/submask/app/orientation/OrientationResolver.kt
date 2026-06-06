package com.submask.app.orientation

import com.submask.app.config.MaskRect
import kotlin.math.roundToInt

object OrientationResolver {
    fun resolve(bounds: ScreenBounds): ScreenOrientation {
        return if (bounds.width > bounds.height) {
            ScreenOrientation.LANDSCAPE
        } else {
            ScreenOrientation.PORTRAIT
        }
    }

    fun defaultRect(orientation: ScreenOrientation, bounds: ScreenBounds): MaskRect {
        return when (orientation) {
            ScreenOrientation.PORTRAIT -> {
                val width = (bounds.width * 0.80f).roundToInt()
                val height = (bounds.height * 0.08f).roundToInt()
                val x = ((bounds.width - width) / 2f).roundToInt()
                val y = (bounds.height * 0.725f).roundToInt()
                MaskRect(x = x, y = y, width = width, height = height)
            }
            ScreenOrientation.LANDSCAPE -> {
                val width = (bounds.width * 0.70f).roundToInt()
                val height = (bounds.height * 0.08f).roundToInt()
                val x = ((bounds.width - width) / 2f).roundToInt()
                val y = (bounds.height * 0.74f).roundToInt()
                MaskRect(x = x, y = y, width = width, height = height)
            }
        }
    }
}
