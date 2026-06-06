package com.submask.app.config

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayConfigKeysTest {
    @Test
    fun keysSeparatePortraitAndLandscapeGeometry() {
        assertEquals("portrait_x", OverlayConfigKeys.portraitX)
        assertEquals("landscape_x", OverlayConfigKeys.landscapeX)
        assertEquals("portrait_initialized", OverlayConfigKeys.portraitInitialized)
        assertEquals("landscape_initialized", OverlayConfigKeys.landscapeInitialized)
    }
}
