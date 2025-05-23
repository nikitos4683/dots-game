package org.dots.game.core

enum class MoveMode {
    Next,
    First,
    Second;

    fun getMovePlayer(): Player? {
        return when (this) {
            Next -> null
            First -> Player.First
            Second -> Player.Second
        }
    }
}