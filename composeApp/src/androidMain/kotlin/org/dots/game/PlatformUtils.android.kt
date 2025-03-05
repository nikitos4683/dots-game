package org.dots.game

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual val isDesktop: Boolean
    get() = false

@Composable
actual fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) {}

@Composable
actual fun HorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) {}

actual fun readFile(filePath: String): String = error("Not supported")