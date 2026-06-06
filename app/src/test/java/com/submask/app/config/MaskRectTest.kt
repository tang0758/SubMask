package com.submask.app.config

import com.submask.app.orientation.ScreenBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class MaskRectTest {
    @Test
    fun clampsRectInsideScreenBounds() {
        val rect = MaskRect(x = 950, y = 1800, width = 300, height = 300)
        val clamped = rect.clampTo(ScreenBounds(1080, 1920), minWidth = 80, minHeight = 48)
        assertEquals(MaskRect(x = 780, y = 1620, width = 300, height = 300), clamped)
    }

    @Test
    fun expandsTooSmallRectToMinimumSize() {
        val rect = MaskRect(x = 20, y = 30, width = 10, height = 10)
        val clamped = rect.clampTo(ScreenBounds(1080, 1920), minWidth = 80, minHeight = 48)
        assertEquals(MaskRect(x = 20, y = 30, width = 80, height = 48), clamped)
    }

    @Test
    fun clampsNegativePositionToZero() {
        val rect = MaskRect(x = -40, y = -20, width = 200, height = 80)
        val clamped = rect.clampTo(ScreenBounds(1080, 1920), minWidth = 80, minHeight = 48)
        assertEquals(MaskRect(x = 0, y = 0, width = 200, height = 80), clamped)
    }
}
