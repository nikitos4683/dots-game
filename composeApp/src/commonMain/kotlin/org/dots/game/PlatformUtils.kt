package org.dots.game

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect val isDesktop: Boolean

@Composable
expect fun HorizontalScrollbar(scrollState: ScrollState, modifier: Modifier)

@Composable
expect fun VerticalScrollbar(scrollState: ScrollState, modifier: Modifier)

expect fun readFile(filePath: String): String