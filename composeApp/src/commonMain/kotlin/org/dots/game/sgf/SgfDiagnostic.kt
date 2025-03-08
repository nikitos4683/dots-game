package org.dots.game.sgf

data class SgfDiagnostic(
    val message: String,
    val lineColumn: LineColumn,
    val severity: SgfDiagnosticSeverity = SgfDiagnosticSeverity.Error,
) {
    override fun toString(): String {
        return "$severity at $lineColumn: $message"
    }
}

enum class SgfDiagnosticSeverity {
    Warning,
    Error,
    Critical,
}