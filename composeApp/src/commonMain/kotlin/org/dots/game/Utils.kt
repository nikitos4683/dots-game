package org.dots.game

import org.dots.game.core.Game
import org.dots.game.sgf.SgfConverter
import org.dots.game.sgf.SgfParser

fun openSgf(sgfPathOrContent: String, diagnosticReporter: ((String) -> Unit) = { }): Game? {
    try {
        val sgf = readFileIfExists(sgfPathOrContent) ?: sgfPathOrContent

        val sgfParseTree = SgfParser.parse(sgf) { parseDiagnostic ->
            diagnosticReporter(parseDiagnostic.toString())
        }
        val games = SgfConverter.convert(sgfParseTree) { convertDiagnostic ->
            diagnosticReporter(convertDiagnostic.toString())
        }
        if (games.size != 1) {
            diagnosticReporter("Only single game inside one SGF is supported.")
        }
        return games.firstOrNull()
    } catch (e: Exception) {
        diagnosticReporter("${e.message}")
    }
    return null
}