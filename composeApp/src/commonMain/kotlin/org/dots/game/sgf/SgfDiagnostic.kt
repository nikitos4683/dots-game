package org.dots.game.sgf

data class SgfDiagnostic(
    val message: String,
    val textSpan: TextSpan?,
    val severity: SgfDiagnosticSeverity = SgfDiagnosticSeverity.Error,
) {
    override fun toString(): String = render(message, textSpan, severity)
}

enum class SgfDiagnosticSeverity {
    Warning,
    Error,
    Critical,
}

data class SgfLineColumnDiagnostic(
    val message: String,
    val lineColumn: LineColumn?,
    val severity: SgfDiagnosticSeverity = SgfDiagnosticSeverity.Error,
) {
    override fun toString(): String = render(message, lineColumn, severity)
}

fun SgfDiagnostic.toLineColumnDiagnostic(lineOffsets: List<Int>): SgfLineColumnDiagnostic =
    SgfLineColumnDiagnostic(message, textSpan?.start?.getLineColumn(lineOffsets), severity)

private fun render(message: String, textSpan: Any?, severity: SgfDiagnosticSeverity): String {
    val lineColumnSuffix = if (textSpan != null) " at $textSpan" else ""
    return "$severity$lineColumnSuffix: $message"
}