package org.dots.game

import androidx.compose.ui.graphics.Color
import org.dots.game.core.Player

class UiSettings(
    val playerFirstColor: Color = Color.Blue,
    val playerSecondColor: Color = Color.Red,
) {
    companion object {
        val Standard = UiSettings()
    }

    fun toColor(player: Player): Color = if (player == Player.First) playerFirstColor else playerSecondColor
}
