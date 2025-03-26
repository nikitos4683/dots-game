package org.dots.game

import org.dots.game.core.Game
import org.dots.game.sgf.Sgf
import org.dots.game.sgf.SgfDiagnostic
import org.dots.game.sgf.SgfDiagnosticSeverity

fun openOrLoadSgf(sgfPathOrContent: String, diagnosticReporter: ((SgfDiagnostic) -> Unit) = { }): Pair<String?, Game?> {
    try {
        val fileInfo = readFileIfExists(sgfPathOrContent)
        val sgf = fileInfo?.content ?: sgfPathOrContent
        return fileInfo?.name to Sgf.parseAndConvert(sgf, onlySingleGameSupported = true, diagnosticReporter).firstOrNull()
    } catch (e: Exception) {
        diagnosticReporter(SgfDiagnostic(e.message ?: e.toString(), textSpan = null, SgfDiagnosticSeverity.Critical))
    }
    return null to null
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
