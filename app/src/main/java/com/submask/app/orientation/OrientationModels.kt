package com.submask.app.orientation

enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE
}

data class ScreenBounds(
    val width: Int,
    val height: Int
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
    }
}
