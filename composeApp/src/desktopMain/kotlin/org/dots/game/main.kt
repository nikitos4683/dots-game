package org.dots.game

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DotsGame",
        state = WindowState(width = 1280.dp, height = 1024.dp)
    ) {
        App()
    }
}