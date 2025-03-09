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