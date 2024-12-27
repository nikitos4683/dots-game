package org.dots.game.core

class Rules(
    val width: Int = 39,
    val height: Int = 32,
    val captureByBorder: Boolean = false,
    val captureEmptyBase: Boolean = false,
    val initialPosition: InitialPosition = InitialPosition.Cross,
) {
    companion object {
        val Standard = Rules()
    }
}

enum class InitialPosition {
    Empty,
    Cross
}