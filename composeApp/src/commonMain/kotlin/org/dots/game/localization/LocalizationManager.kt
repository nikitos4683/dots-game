package org.dots.game.localization

import androidx.compose.runtime.*
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * Manages localization state and persistence.
 * Pure Kotlin implementation that works on all platforms including WASM.
 */
class LocalizationManager(private val settings: Settings) {
    private val languageKey = "app_language"

    /**
     * Current language state.
     */
    var currentLanguage by mutableStateOf(loadLanguage())
        private set

    /**
     * Current strings based on selected language.
     */
    val strings: Strings
        @Composable
        get() = remember(currentLanguage) { getStringsForLanguage(currentLanguage) }

    /**
     * Get strings for a specific language (non-composable).
     */
    fun getStringsForLanguage(language: Language): Strings = when (language) {
        Language.English -> EnglishStrings
        Language.Russian -> RussianStrings
    }

    /**
     * Change the current language and persist the selection.
     */
    fun setLanguage(language: Language) {
        currentLanguage = language
        settings[languageKey] = language.code
    }

    /**
     * Load language from settings, defaults to English.
     */
    private fun loadLanguage(): Language {
        val code = settings.getStringOrNull(languageKey) ?: return Language.English
        return Language.fromCode(code)
    }
}

/**
 * CompositionLocal for accessing localization throughout the app.
 */
val LocalLocalizationManager = compositionLocalOf<LocalizationManager> {
    error("No LocalizationManager provided")
}

/**
 * Convenience composable for accessing current strings.
 */
val LocalStrings: Strings
    @Composable
    get() = LocalLocalizationManager.current.strings
