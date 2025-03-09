package org.dots.game.sgf

data class SgfDiagnostic(
    val message: String,
    val lineColumn: LineColumn,
    val severity: SgfDiagnosticSeverity = SgfDiagnosticSeverity.Error,
) {
    override fun toString(): String {
        val lineColumnSuffix = if (lineColumn != LineColumn.NONE) " at $lineColumn" else ""
        return "$severity$lineColumnSuffix: $message"
    }
}

enum class SgfDiagnosticSeverity {
    Warning,
    Error,
    Critical,
}