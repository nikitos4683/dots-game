package org.dots.game

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.net.URI
import java.util.prefs.Preferences

actual val appSettings: Settings = PreferencesSettings(Preferences.userRoot())

@Composable
actual fun HorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) = HorizontalScrollbar(rememberScrollbarAdapter(scrollState), modifier)

@Composable
actual fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) = VerticalScrollbar(rememberScrollbarAdapter(scrollState), modifier)

actual fun readFileText(filePath: String): String = File(filePath).readText()

actual fun fileExists(filePath: String): Boolean = File(filePath).exists()

actual suspend fun downloadFileText(fileUrl: String): String = URI.create(fileUrl).toURL().openStream().use {
    it.readBytes().decodeToString()
}