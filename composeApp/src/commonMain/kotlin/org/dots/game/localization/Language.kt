package org.dots.game.localization

/**
 * Supported languages in the application.
 */
enum class Language(val code: String, val displayName: String) {
    English("en", "English"),
    Russian("ru", "Русский");

    companion object {
        /**
         * Get Language by code, defaults to English if not found.
         */
        fun fromCode(code: String): Language =
            entries.firstOrNull { it.code == code } ?: English
    }
}
