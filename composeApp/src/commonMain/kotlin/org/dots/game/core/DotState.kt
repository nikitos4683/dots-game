package org.dots.game.core

import org.dots.game.core.Player.First
import org.dots.game.core.Player.Second
import kotlin.jvm.JvmInline

@JvmInline
value class DotState(val value: Int) {
    companion object {
        val Empty = DotState(0)
        val Border = DotState(DotStateFlags.Border.value)

        private val PlacedTerritoryMask = DotStateFlags.Placed.value or DotStateFlags.Territory.value
        private val PlacedMask = DotStateFlags.Placed.value or DotStateFlags.PlacedPlayer.value
        private val ActiveMask = DotStateFlags.Placed.value or DotStateFlags.Territory.value or DotStateFlags.PlacedPlayer.value
        private val TerritoryMask = DotStateFlags.Territory.value or DotStateFlags.TerritoryPlayer.value
        private val InvertTerritoryMask = TerritoryMask.inv()
        private val EmptyTerritoryMask = DotStateFlags.EmptyTerritory.value or DotStateFlags.EmptyTerritoryPlayer.value
    }

    fun checkPlacedOrTerritory(): Boolean {
        return value and PlacedTerritoryMask != 0
    }

    fun checkPlaced(): Boolean {
        return value and DotStateFlags.Placed.value != 0
    }

    fun checkPlaced(playerPlacedValue: DotState): Boolean {
        return value and PlacedMask == playerPlacedValue.value
    }

    fun checkActive(playerActiveValue: DotState): Boolean {
        return value and ActiveMask == playerActiveValue.value || value and DotStateFlags.Border.value != 0
    }

    fun checkTerritory(): Boolean {
        return value and DotStateFlags.Territory.value != 0
    }

    fun checkTerritory(playerAndTerritory: DotState): Boolean {
        return value and TerritoryMask == playerAndTerritory.value
    }

    fun getTerritoryPlayer(): Player {
        return if (value and DotStateFlags.TerritoryPlayer.value == 0) First else Second
    }

    fun checkWithinEmptyTerritory(): Boolean {
        return value and DotStateFlags.EmptyTerritory.value != 0
    }

    fun checkWithinEmptyTerritory(player: Player): Boolean {
        return value and EmptyTerritoryMask ==
                (if (player == First) 0 else DotStateFlags.EmptyTerritoryPlayer.value) or DotStateFlags.EmptyTerritory.value
    }

    fun getEmptyTerritoryPlayer(): Player {
        return if (value and DotStateFlags.EmptyTerritoryPlayer.value == 0) First else Second
    }

    fun checkBorder(): Boolean {
        return value and DotStateFlags.Border.value != 0
    }

    fun getPlacedPlayer(): Player {
        return if (value and DotStateFlags.PlacedPlayer.value == 0) First else Second
    }

    fun setTerritory(playerAndTerritory: DotState): DotState {
        return DotState(value and InvertTerritoryMask or playerAndTerritory.value)
    }

    override fun toString(): String {
        return buildString {
            if (value and DotStateFlags.Placed.value != 0) {
                append("Placed: Player ")
                append(if (value and DotStateFlags.PlacedPlayer.value == 0) "0" else "1")
                append("; ")
            }

            if (value and DotStateFlags.Territory.value != 0) {
                append("Territory: Player")
                append(if (value and DotStateFlags.TerritoryPlayer.value == 0) "0" else "1")
                append("; ")
            }

            if (value and DotStateFlags.EmptyTerritory.value != 0) {
                append("WithinEmptyTerritory: Player")
                append(if (value and DotStateFlags.EmptyTerritoryPlayer.value == 0) "0" else "1")
                append("; ")
            }

            if (value and DotStateFlags.Border.value != 0) {
                append("Border; ")
            }
        }
    }
}

private enum class DotStateFlags(val value: Int) {
    Placed(0x1),
    PlacedPlayer(0x2),
    Territory(0x4),
    TerritoryPlayer(0x8),
    EmptyTerritory(0x10),
    EmptyTerritoryPlayer(0x20),
    Border(0x40),
}

fun Player.createPlacedState(): DotState {
    return DotState(DotStateFlags.Placed.value or (if (this == First) 0 else DotStateFlags.PlacedPlayer.value))
}

fun Player.createTerritoryState(): DotState {
    return DotState(DotStateFlags.Territory.value or (if (this == First) 0 else DotStateFlags.TerritoryPlayer.value))
}

fun Player.createEmptyTerritoryState(): DotState {
    return DotState(DotStateFlags.EmptyTerritory.value or (if (this == First) 0 else DotStateFlags.EmptyTerritoryPlayer.value))
}

const val FIRST_PLAYER_MARKER = '*'
const val SECOND_PLAYER_MARKER = '+'
const val TERRITORY_EMPTY_MARKER = '^'
const val EMPTY_TERRITORY_MARKER = '`'
const val EMPTY_POSITION = '.'

val playerMarker = mapOf(First to FIRST_PLAYER_MARKER, Second to SECOND_PLAYER_MARKER)
