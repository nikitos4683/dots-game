package org.dots.game

import org.dots.game.core.Game
import org.dots.game.sgf.LineColumn
import org.dots.game.sgf.Sgf
import org.dots.game.sgf.SgfDiagnostic
import org.dots.game.sgf.SgfDiagnosticSeverity

fun openOrLoadSgf(sgfPathOrContent: String, diagnosticReporter: ((SgfDiagnostic) -> Unit) = { }): Game? {
    try {
        val sgf = readFileIfExists(sgfPathOrContent) ?: sgfPathOrContent
        return Sgf.parseAndConvert(sgf, onlySingleGameSupported = true, diagnosticReporter).firstOrNull()
    } catch (e: Exception) {
        diagnosticReporter(SgfDiagnostic(e.message ?: e.toString(), LineColumn.NONE, SgfDiagnosticSeverity.Critical))
    }
    return null
}