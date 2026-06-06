package com.submask.app.config

data class MaskRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

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
