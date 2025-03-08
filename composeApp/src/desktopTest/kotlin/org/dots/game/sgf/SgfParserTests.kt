package org.dots.game.sgf

import kotlin.test.Test
import kotlin.test.assertEquals

class SgfParserTests {
    @Test
    fun emptyOrWhitespace() {
        val emptySgf = parse("")
        assertEquals(TextSpan(0, 0), emptySgf.textSpan)

        val whitespaceSgf = parse("    ")
        assertEquals(TextSpan(4, 0), whitespaceSgf.textSpan)

        val incorrectSgf = parse("  ---",
            SgfDiagnostic("Unrecognized text `---`", LineColumn(1, 3))
        )
        assertEquals(TextSpan(2, 3), incorrectSgf.textSpan)
    }

    @Test
    fun tokens() {
        val gameTree = parse("(;GC[info])").gameTree.single()
        checkTokens(LParenToken(TextSpan(0, 1)), gameTree.lParen)
        checkTokens(RParenToken(TextSpan(10, 1)), gameTree.rParen)

        val node = gameTree.nodes.single()
        checkTokens(SemicolonToken(TextSpan(1, 1)), node.semicolon)

        val property = node.properties.single()
        checkTokens(IdentifierToken("GC", TextSpan(2, 2)), property.identifier)

        val propertyValue = property.value.single()
        checkTokens(LSquareBracketToken(TextSpan(4, 1)), propertyValue.lSquareBracket)
        checkTokens(RSquareBracketToken(TextSpan(9, 1)), propertyValue.rSquareBracket)
    }

    @Test
    fun unparsedText() {
        fun check(expectedToken: UnparsedTextToken, text: String, vararg diagnostics: SgfDiagnostic = arrayOf()) {
            val actualToken = parse(text, *diagnostics).unparsedText!!
            checkTokens(expectedToken, actualToken)
        }

        check(UnparsedTextToken("---", TextSpan(0, 3)), "---",
            SgfDiagnostic("Unrecognized text `---`", LineColumn(1, 1))
        )
        check(UnparsedTextToken("---", TextSpan(1, 3)), "(---",
            SgfDiagnostic("Missing `)`", LineColumn(1, 2)),
            SgfDiagnostic("Unrecognized text `---`", LineColumn(1, 2)),
        )
        check(UnparsedTextToken("---", TextSpan(2, 3)), "(;---",
            SgfDiagnostic("Missing `)`", LineColumn(1, 3)),
            SgfDiagnostic("Unrecognized text `---`", LineColumn(1, 3)),
        )
        check(UnparsedTextToken("---", TextSpan(4, 3)), "(;GM---",
            SgfDiagnostic("Missing `)`", LineColumn(1, 5)),
            SgfDiagnostic("Unrecognized text `---`", LineColumn(1, 5)),
        )
        check(UnparsedTextToken("---", TextSpan(2, 3)), "  ---",
            SgfDiagnostic("Unrecognized text `---`", LineColumn(1, 3)),
        )
    }

    @Test
    fun missingTokens() {
        checkTokens(
            RParenToken(TextSpan(10, 0)),
            parse("(;GC[info]",
                SgfDiagnostic("Missing `)`", LineColumn(1, 11))
                ).gameTree.single().rParen
        )

        val gameTreeWithMissingOuterRParen = parse("(;GC[info](;B[ee])",
            SgfDiagnostic("Missing `)`", LineColumn(1, 19))
            ).gameTree.single()
        checkTokens(
            RParenToken(TextSpan(17, 1)),
            gameTreeWithMissingOuterRParen.childrenGameTrees.single().rParen
        )
        checkTokens(
            RParenToken(TextSpan(18, 0)),
            gameTreeWithMissingOuterRParen.rParen
        )

        val missingRSquare = parse("(;GC[",
            SgfDiagnostic("Missing `]`", LineColumn(1, 6)),
            SgfDiagnostic("Missing `)`", LineColumn(1, 6)
        )).gameTree.single().nodes.single().properties.single().value.single().rSquareBracket
        checkTokens(RSquareBracketToken(TextSpan(5, 0)), missingRSquare)
    }

    @Test
    fun propertyValue() {
        fun checkValue(expectedToken: SgfToken, input: String, vararg diagnostics: SgfDiagnostic) {
            val root = parse(input, *diagnostics)
            val propertyValueType =
                root.gameTree.single().nodes.single().properties.single().value.single().propertyValueToken
            checkTokens(expectedToken, propertyValueType)
        }

        checkValue(PropertyValueToken("", TextSpan(5, 0)), "(;GC[])")
        checkValue(PropertyValueToken("text", TextSpan(5, 4)), """(;GC[text])""")
        checkValue(PropertyValueToken("a:b", TextSpan(5, 3)), """(;GC[a:b])""")
        checkValue(PropertyValueToken("a\nb", TextSpan(5, 3)), "(;GC[a\nb])")
        checkValue(PropertyValueToken("""\]""", TextSpan(5, 2)), """(;GC[\]])""")
        checkValue(PropertyValueToken("""\\""", TextSpan(5, 2)), """(;GC[\\])""")

        // Check escaping at the end
        checkValue(
            PropertyValueToken("""\""", TextSpan(5, 1)), """(;GC[\""",
            SgfDiagnostic("Missing `]`", LineColumn(1, 7)),
            SgfDiagnostic("Missing `)`", LineColumn(1, 7))
        )
    }

    @Test
    fun diagnosticReporter() {
        parse("(;GC[info]---",
            SgfDiagnostic("Missing `)`", LineColumn(1, 11)),
            SgfDiagnostic("Unrecognized text `---`", LineColumn(1, 11))
        )
        parse("""(;GC[\""",
            SgfDiagnostic("Missing `]`", LineColumn(1, 7)),
            SgfDiagnostic("Missing `)`", LineColumn(1, 7)),
        )
    }

    @Test
    fun whitespaces() {
        val sgf = parse(" ( ;\nGC [info1 info2 ] ) ---",
            SgfDiagnostic("Unrecognized text `---`", LineColumn(2, 21)),
        )
        assertEquals(TextSpan(25, 3), sgf.unparsedText!!.textSpan)

        val gameTree = sgf.gameTree.single()
        assertEquals(TextSpan(1, 23), gameTree.textSpan)

        val node = gameTree.nodes.single()
        assertEquals(TextSpan(3, 19), node.textSpan)

        val property = node.properties.single()
        assertEquals(TextSpan(5, 17), property.textSpan)

        val propertyValue = property.value.single()
        assertEquals(TextSpan(8, 14), propertyValue.textSpan)

        checkTokens(PropertyValueToken("info1 info2 ", TextSpan(9, 12)), propertyValue.propertyValueToken)
    }

    private fun parse(input: String, vararg expectedDiagnostics: SgfDiagnostic): SgfRoot {
        val actualDiagnostics = mutableListOf<SgfDiagnostic>()
        val sgfRoot = SgfParser.parse(input) {
            actualDiagnostics.add(it)
        }
        assertEquals(expectedDiagnostics.toList(), actualDiagnostics)
        return sgfRoot
    }

    private fun checkTokens(expectedToken: SgfToken, actualToken: SgfToken) {
        assertEquals(expectedToken::class, actualToken::class)
        assertEquals(expectedToken.value, actualToken.value)
        assertEquals(expectedToken.textSpan, actualToken.textSpan)
    }
}