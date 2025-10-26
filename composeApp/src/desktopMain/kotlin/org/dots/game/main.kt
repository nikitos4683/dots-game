package org.dots.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.dots.game.core.Games

fun main() = application {
    val previousWindowsState = loadWindowsState()
    val windowState = rememberWindowState(
        placement = previousWindowsState.placement,
        position = previousWindowsState.position,
        size = previousWindowsState.size,
    )

    var games by remember { mutableStateOf<Games?>(null) }

    Window(
        onCloseRequest = {
            saveWindowsState(windowState)
            saveGamesIfNeeded(games)
            exitApplication()
        },
        title = "Dots Game",
        state = windowState
    ) {
        App {
            games = it
        }
    }
}