package com.submask.app.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayOpacityControlTest {
    @Test
    fun `converts percent to opacity`() {
        assertEquals(0.72f, OverlayOpacityControl.opacityFromPercent(72), 0.001f)
    }

    @Test
    fun `clamps opacity percent to supported range`() {
        assertEquals(0.20f, OverlayOpacityControl.opacityFromPercent(1), 0.001f)
        assertEquals(1.00f, OverlayOpacityControl.opacityFromPercent(120), 0.001f)
    }

    @Test
    fun `converts opacity to percent`() {
        assertEquals(72, OverlayOpacityControl.percentFromOpacity(0.724f))
    }
}
