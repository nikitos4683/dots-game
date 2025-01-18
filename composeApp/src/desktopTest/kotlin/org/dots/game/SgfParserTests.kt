package org.dots.game

import org.dots.game.sgf.PropertyValue
import org.dots.game.sgf.UnparsedText
import org.dots.game.sgf.RParenToken
import org.dots.game.sgf.RSquareBracketToken
import org.dots.game.sgf.SgfParser
import org.dots.game.sgf.SgfToken
import org.dots.game.sgf.TextSpan
import org.dots.game.sgf.PropertyValueToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SgfParserTests {
    @Test
    fun spaces() {
        // TDOO
    }

    @Test
    fun unparsedText() {
        fun check(expectedToken: UnparsedText, text: String) {
            val actualToken = SgfParser.parse(text).unparsedText!!
            checkTokens(expectedToken, actualToken)
        }

        check(UnparsedText("---", TextSpan(0, 3)), "---")
        check(UnparsedText("---", TextSpan(1, 3)), "(---")
        check(UnparsedText("---", TextSpan(2, 3)), "(;---")
        check(UnparsedText("---", TextSpan(4, 3)), "(;GM---")
    }

    @Test
    fun missingTokens() {
        checkTokens(
            RParenToken(TextSpan(10, 0)),
            SgfParser.parse("""(;GC[info]""").gameTree.single().rParen
        )

        val gameTreeWithMissingOuterRParen = SgfParser.parse("""(;GC[info](;B[ee])""").gameTree.single()
        checkTokens(
            RParenToken(TextSpan(17, 1)),
            gameTreeWithMissingOuterRParen.childrenGameTrees.single().rParen
        )
        checkTokens(
            RParenToken(TextSpan(18, 0)),
            gameTreeWithMissingOuterRParen.rParen
        )

        val missingRSquare = SgfParser.parse("""(;GC[""").gameTree.single().nodes.single().properties.single().value.single().rSquareBracket
        checkTokens(RSquareBracketToken(TextSpan(5, 0)), missingRSquare)
    }

    @Test
    fun propertyValue() {
        fun checkValue(expectedToken: SgfToken, input: String) {
            val propertyValueType = parseAndGetPropertyValue(input).propertyValueToken!!
            checkTokens(expectedToken, propertyValueType)
        }

        assertNull(parseAndGetPropertyValue("""(;GC[])""").propertyValueToken)

        checkValue(PropertyValueToken("text", TextSpan(5, 4)), """(;GC[text])""")
        checkValue(PropertyValueToken("\n", TextSpan(5, 1)), "(;GC[\n])")
        checkValue(PropertyValueToken("]", TextSpan(5, 2)), """(;GC[\]])""")
        checkValue(PropertyValueToken("\\", TextSpan(5, 2)), """(;GC[\\])""")
        checkValue(PropertyValueToken("a:b", TextSpan(5, 4)), """(;GC[a\:b])""")

        // Check escaping at the end
        checkValue(PropertyValueToken("", TextSpan(5, 1)), """(;GC[\""")
    }

    @Test
    fun errorReporter() {
        fun checkErrors(expectedErrorTokens: List<SgfToken>, input: String) {
            var currentErrorIndex = 0
            SgfParser.parse(input) {
                checkTokens(expectedErrorTokens[currentErrorIndex++], it)
            }
            assertEquals(currentErrorIndex, expectedErrorTokens.size)
        }

        checkErrors(listOf(UnparsedText("---", TextSpan(0, 3))), "---")
    }

    private fun parseAndGetPropertyValue(input: String): PropertyValue {
        val root = SgfParser.parse(input)
        return root.gameTree.single().nodes.single().properties.single().value.single()
    }

    private fun checkTokens(expectedToken: SgfToken, actualToken: SgfToken) {
        assertEquals(expectedToken::class, actualToken::class)
        assertEquals(expectedToken.value, actualToken.value)
        assertEquals(expectedToken.textSpan, actualToken.textSpan)
    }
}