package org.dots.game

import org.dots.game.core.Game
import org.dots.game.sgf.Sgf
import org.dots.game.sgf.SgfDiagnostic
import org.dots.game.sgf.SgfDiagnosticSeverity

suspend fun openOrLoadSgf(sgfPathOrContent: String, diagnosticReporter: ((SgfDiagnostic) -> Unit) = { }): Pair<String?, Game?> {
    try {
        val inputType = getInputType(sgfPathOrContent)
        val fileName: String?
        var sgf: String?

        when (inputType) {
            InputType.Content -> {
                fileName = null
                sgf = sgfPathOrContent
            }

            is InputType.File -> {
                if (inputType.isIncorrect) {
                    diagnosticReporter(SgfDiagnostic("Incorrect file `${inputType.name}`. The only .sgf files are supported", textSpan = null))
                    return null to null
                }

                fileName = inputType.name
                sgf = readFileText(inputType.refinedPath)
            }

            is InputType.Url -> {
                if (inputType.isIncorrect) {
                    diagnosticReporter(SgfDiagnostic("Incorrect url. The only `$zagramLinkPrefix` is supported", textSpan = null))
                    return null to null
                }

                fileName = inputType.name
                sgf = downloadFileText(inputType.refinedPath)
            }

            is InputType.Other -> {
                diagnosticReporter(SgfDiagnostic("Unrecognized input type. Insert a path to .sgf file or a link to zagram.org game", textSpan = null))
                fileName = null
                sgf = null
            }
        }

        return fileName to sgf?.let { Sgf.parseAndConvert(it, onlySingleGameSupported = true, diagnosticReporter).firstOrNull() }
    } catch (e: Exception) {
        diagnosticReporter(SgfDiagnostic(e.message ?: e.toString(), textSpan = null, SgfDiagnosticSeverity.Critical))
    }
    return null to null
}

internal sealed class InputType {
    object Content : InputType()

    sealed class InputTypeWithName(val refinedPath: String, val name: String, val isIncorrect: Boolean) : InputType() {
        override fun equals(other: Any?): Boolean {
            if (!super.equals(other)) return false
            other as InputTypeWithName
            return isIncorrect == other.isIncorrect && refinedPath == other.refinedPath && name == other.name
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + isIncorrect.hashCode()
            result = 31 * result + refinedPath.hashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }

    class File(refinedPath: String, name: String, isIncorrect: Boolean = false) : InputTypeWithName(refinedPath, name, isIncorrect)
    class Url(refinedPath: String, name: String, isIncorrect: Boolean = false) : InputTypeWithName(refinedPath, name, isIncorrect)

    object Other : InputType()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}

private const val zagramLinkPrefix = "https://zagram.org/eidokropki/"
private const val zagramDownloadLinkPrefix = zagramLinkPrefix + "backend/download.py?id="
private const val zagramIdGroupName = "id"
private val zagramIdRegex = Regex("""zagram\d+""")
private val zagramDownloadLinkRegex = Regex(Regex.escape(zagramDownloadLinkPrefix) + """(?<$zagramIdGroupName>${zagramIdRegex.pattern})""")
private val zagramGameViewLinkRegex = Regex(Regex.escape(zagramLinkPrefix) + """index\.html#url:(?<$zagramIdGroupName>${zagramIdRegex.pattern})""")
private val filePathRegex = Regex(""".*[\\/](?!.*[\\/])(.*)""")

internal fun getInputType(input: String): InputType {
    val indexOfSgfMarker = input.indexOf("(;")
    if (input.isEmpty() || indexOfSgfMarker != -1 && input.subSequence(0, indexOfSgfMarker).all { it.isWhitespace() }) {
        return InputType.Content
    }

    if (input.startsWith("https://")) {
        val zagramDownloadLinkMatch = zagramDownloadLinkRegex.matchEntire(input)
        if (zagramDownloadLinkMatch != null) {
            return InputType.Url(input, zagramDownloadLinkMatch.groups[zagramIdGroupName]!!.value)
        }

        val zagramGameViewLinkMatch = zagramGameViewLinkRegex.matchEntire(input)
        if (zagramGameViewLinkMatch != null) {
            val id = zagramGameViewLinkMatch.groups[zagramIdGroupName]!!.value
            return InputType.Url(zagramDownloadLinkPrefix + id, id)
        }

        return InputType.Url(input, "", isIncorrect = true)
    }

    if (zagramIdRegex.matchEntire(input) != null) {
        return InputType.Url(zagramDownloadLinkPrefix + input, input)
    }

    fun extractFileName(filePath: String): String {
        return filePath.substring(filePath.lastIndexOfAny(charArrayOf('/', '\\')) + 1)
    }

    val refinedPath = input.removeSurrounding("\"")
    if (refinedPath.endsWith(".sgf")) {
        return InputType.File(refinedPath, extractFileName(refinedPath))
    }

    if (fileExists(refinedPath) || filePathRegex.matches(refinedPath)) {
        return InputType.File(refinedPath, extractFileName(refinedPath), isIncorrect = true)
    }

    return InputType.Other
}

fun splitByUppercase(input: String): String {
    var prevUpperCase = false
    var currentWordIndex = 0

    return buildString {
        for ((index, char) in input.withIndex()) {
            prevUpperCase = if (char.isUpperCase()) {
                if (!prevUpperCase) {
                    if (index != 0) {
                        append(input.subSequence(currentWordIndex, index))
                        append(' ')
                    }
                    currentWordIndex = index
                }
                true
            } else {
                false
            }
        }

        append(input.subSequence(currentWordIndex, input.length))
    }
}
