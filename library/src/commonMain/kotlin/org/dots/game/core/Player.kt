package org.dots.game.core

enum class Player(val value: Int) {
    None(DotState.Empty.value),
    First(DotState.Player1Placed.value),
    Second(DotState.Player2Placed.value),
    Both(DotState.BothPlayersPlaced.value);

    fun opposite(): Player = when (this) {
        First -> Second
        Second -> First
        None -> Both
        Both -> None
    }

    operator fun plus(other: Player): Player = when (this) {
        None -> other
        First -> if (other == Second) Both else First
        Second -> if (other == First) Both else Second
        Both -> Both
    }
}