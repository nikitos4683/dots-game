package org.dots.game

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.russhwolf.settings.Settings

expect val appSettings: Settings

@Composable
expect fun HorizontalScrollbar(scrollState: ScrollState, modifier: Modifier)

@Composable
expect fun VerticalScrollbar(scrollState: ScrollState, modifier: Modifier)

expect fun readFileText(filePath: String): String

expect fun fileExists(filePath: String): Boolean

expect suspend fun downloadFileText(fileUrl: String): String