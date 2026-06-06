package com.submask.app.orientation

import com.submask.app.config.MaskRect
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationResolverTest {
    @Test
    fun resolvesPortraitWhenHeightIsGreaterThanOrEqualToWidth() {
        assertEquals(ScreenOrientation.PORTRAIT, OrientationResolver.resolve(ScreenBounds(1080, 1920)))
        assertEquals(ScreenOrientation.PORTRAIT, OrientationResolver.resolve(ScreenBounds(1000, 1000)))
    }

    @Test
    fun resolvesLandscapeWhenWidthIsGreaterThanHeight() {
        assertEquals(ScreenOrientation.LANDSCAPE, OrientationResolver.resolve(ScreenBounds(1920, 1080)))
    }

    @Test
    fun portraitDefaultUsesLowerSubtitleArea() {
        val rect = OrientationResolver.defaultRect(ScreenOrientation.PORTRAIT, ScreenBounds(1080, 1920))
        assertEquals(MaskRect(x = 108, y = 1392, width = 864, height = 154), rect)
    }

    @Test
    fun landscapeDefaultUsesWiderShorterLowerSubtitleArea() {
        val rect = OrientationResolver.defaultRect(ScreenOrientation.LANDSCAPE, ScreenBounds(1920, 1080))
        assertEquals(MaskRect(x = 288, y = 799, width = 1344, height = 86), rect)
    }
}
