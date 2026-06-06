package com.submask.app.orientation

import com.submask.app.config.OrientationConfig
import com.submask.app.config.OverlayConfigStore

object OrientationStateSelector {
    fun select(
        orientation: ScreenOrientation,
        bounds: ScreenBounds,
        stored: OrientationConfig
    ): OrientationConfig {
        return if (stored.initialized) {
            stored.copy(
                rect = stored.rect.clampTo(
                    bounds,
                    OverlayConfigStore.MIN_MASK_WIDTH,
                    OverlayConfigStore.MIN_MASK_HEIGHT
                )
            )
        } else {
            stored.copy(rect = OrientationResolver.defaultRect(orientation, bounds))
        }
    }
}
