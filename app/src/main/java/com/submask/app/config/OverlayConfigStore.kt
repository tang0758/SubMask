package com.submask.app.config

import android.content.Context
import android.content.SharedPreferences
import com.submask.app.orientation.OrientationResolver
import com.submask.app.orientation.ScreenBounds
import com.submask.app.orientation.ScreenOrientation

object OverlayConfigKeys {
    const val opacity = "opacity"

    const val portraitX = "portrait_x"
    const val portraitY = "portrait_y"
    const val portraitWidth = "portrait_width"
    const val portraitHeight = "portrait_height"
    const val portraitLocked = "portrait_locked"
    const val portraitInitialized = "portrait_initialized"

    const val landscapeX = "landscape_x"
    const val landscapeY = "landscape_y"
    const val landscapeWidth = "landscape_width"
    const val landscapeHeight = "landscape_height"
    const val landscapeLocked = "landscape_locked"
    const val landscapeInitialized = "landscape_initialized"
}

class OverlayConfigStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("overlay_config", Context.MODE_PRIVATE)

    fun getOpacity(): Float = prefs.getFloat(OverlayConfigKeys.opacity, 0.72f)

    fun setOpacity(opacity: Float) {
        prefs.edit()
            .putFloat(OverlayConfigKeys.opacity, opacity.coerceIn(0.20f, 1.00f))
            .apply()
    }

    fun loadOrientationConfig(
        orientation: ScreenOrientation,
        bounds: ScreenBounds
    ): OrientationConfig {
        val keys = keysFor(orientation)
        val initialized = prefs.getBoolean(keys.initialized, false)
        if (!initialized) {
            return OrientationConfig(
                rect = OrientationResolver.defaultRect(orientation, bounds),
                locked = false,
                initialized = false
            )
        }

        val rect = MaskRect(
            x = prefs.getInt(keys.x, 0),
            y = prefs.getInt(keys.y, 0),
            width = prefs.getInt(keys.width, bounds.width),
            height = prefs.getInt(keys.height, bounds.height)
        ).clampTo(bounds, MIN_MASK_WIDTH, MIN_MASK_HEIGHT)

        return OrientationConfig(
            rect = rect,
            locked = prefs.getBoolean(keys.locked, false),
            initialized = true
        )
    }

    fun saveOrientationConfig(
        orientation: ScreenOrientation,
        config: OrientationConfig
    ) {
        val keys = keysFor(orientation)
        prefs.edit()
            .putInt(keys.x, config.rect.x)
            .putInt(keys.y, config.rect.y)
            .putInt(keys.width, config.rect.width)
            .putInt(keys.height, config.rect.height)
            .putBoolean(keys.locked, config.locked)
            .putBoolean(keys.initialized, true)
            .apply()
    }

    private fun keysFor(orientation: ScreenOrientation): OrientationKeys {
        return when (orientation) {
            ScreenOrientation.PORTRAIT -> OrientationKeys(
                OverlayConfigKeys.portraitX,
                OverlayConfigKeys.portraitY,
                OverlayConfigKeys.portraitWidth,
                OverlayConfigKeys.portraitHeight,
                OverlayConfigKeys.portraitLocked,
                OverlayConfigKeys.portraitInitialized
            )
            ScreenOrientation.LANDSCAPE -> OrientationKeys(
                OverlayConfigKeys.landscapeX,
                OverlayConfigKeys.landscapeY,
                OverlayConfigKeys.landscapeWidth,
                OverlayConfigKeys.landscapeHeight,
                OverlayConfigKeys.landscapeLocked,
                OverlayConfigKeys.landscapeInitialized
            )
        }
    }

    private data class OrientationKeys(
        val x: String,
        val y: String,
        val width: String,
        val height: String,
        val locked: String,
        val initialized: String
    )

    companion object {
        const val MIN_MASK_WIDTH = 80
        const val MIN_MASK_HEIGHT = 48
    }
}
