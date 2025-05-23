package org.dots.game.core

enum class Player {
    First,
    Second;

    fun opposite(): Player = if (this == First) Second else First
}