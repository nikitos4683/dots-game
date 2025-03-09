package org.dots.game.sgf

import org.dots.game.core.Game

object Sgf {
    fun parseAndConvert(sgf: String, onlySingleGameSupported: Boolean = false, diagnosticReporter: (SgfDiagnostic) -> Unit): List<Game> {
        try {
            val sgfParseTree = SgfParser.parse(sgf) { parseDiagnostic ->
                diagnosticReporter(parseDiagnostic)
            }
            val games = SgfConverter.convert(sgfParseTree, warnOnMultipleGames = onlySingleGameSupported) { convertDiagnostic ->
                diagnosticReporter(convertDiagnostic)
            }
            return games
        } catch(e: Exception) {
            diagnosticReporter(SgfDiagnostic(e.message ?: e.toString(), textSpan = null, SgfDiagnosticSeverity.Critical))
        }
        return emptyList()
    }
}