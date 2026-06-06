package com.submask.app.orientation

import com.submask.app.config.MaskRect
import com.submask.app.config.OrientationConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationSwitchingTest {
    @Test
    fun usesDefaultForUninitializedNewOrientation() {
        val landscapeDefault = OrientationResolver.defaultRect(
            ScreenOrientation.LANDSCAPE,
            ScreenBounds(1920, 1080)
        )
        val selected = OrientationStateSelector.select(
            orientation = ScreenOrientation.LANDSCAPE,
            bounds = ScreenBounds(1920, 1080),
            stored = OrientationConfig(
                rect = MaskRect(10, 10, 10, 10),
                locked = false,
                initialized = false
            )
        )
        assertEquals(landscapeDefault, selected.rect)
    }
}
