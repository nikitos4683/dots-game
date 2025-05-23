package org.dots.game

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.russhwolf.settings.Settings
import java.io.File
import java.net.URI

actual var appSettings: Settings? = null

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

actual fun readFileText(filePath: String): String = File(filePath).readText()

actual fun fileExists(filePath: String): Boolean = File(filePath).exists()

actual suspend fun downloadFileText(fileUrl: String): String = URI.create(fileUrl).toURL().openStream().use {
    it.readBytes().decodeToString()
}