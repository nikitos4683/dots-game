package org.dots.game

import org.dots.game.sgf.UnparsedText
import org.dots.game.sgf.PropertyValueType
import org.dots.game.sgf.RParenToken
import org.dots.game.sgf.RSquareBracketToken
import org.dots.game.sgf.SgfParser
import org.dots.game.sgf.SgfToken
import org.dots.game.sgf.TextSpan
import org.dots.game.sgf.ValueTypeToken
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
            gameTreeWithMissingOuterRParen.children.single().rParen
        )
        checkTokens(
            RParenToken(TextSpan(18, 0)),
            gameTreeWithMissingOuterRParen.rParen
        )

        val missingRSquare = SgfParser.parse("""(;GC[""").gameTree.single().nodes.single().properties.single().value.single().rSquareBracketToken
        checkTokens(RSquareBracketToken(TextSpan(5, 0)), missingRSquare)
    }

    @Test
    fun propertyValueType() {
        fun checkSimpleValue(expectedToken: SgfToken, input: String) {
            val propertyValueType = parseAndGetFirstPropertyValue(input)!!.valueTypeToken
            checkTokens(expectedToken, propertyValueType)
        }

        assertNull(parseAndGetFirstPropertyValue("""(;GC[])"""))

        checkSimpleValue(ValueTypeToken("text", TextSpan(5, 4)), """(;GC[text])""")
        checkSimpleValue(ValueTypeToken("\n", TextSpan(5, 1)), "(;GC[\n])")
        checkSimpleValue(ValueTypeToken("]", TextSpan(5, 2)), """(;GC[\]])""")
        checkSimpleValue(ValueTypeToken("\\", TextSpan(5, 2)), """(;GC[\\])""")
        checkSimpleValue(ValueTypeToken("a:b", TextSpan(5, 4)), """(;GC[a\:b])""")

        // Check escaping at the end
        checkSimpleValue(ValueTypeToken("", TextSpan(5, 1)), """(;GC[\""")
    }

    @Test
    fun composePropertyValue() {
        fun checkComposeValue(leftPart: SgfToken, rightPart: SgfToken, input: String) {
            val propertyValue = parseAndGetFirstPropertyValue(input)!!
            checkTokens(leftPart, propertyValue.valueTypeToken)
            checkTokens(rightPart, propertyValue.valueComposeType!!.valueTypeToken!!)
        }

        checkComposeValue(
            ValueTypeToken("a", TextSpan(5, 1)),
            ValueTypeToken("b", TextSpan(7, 1)),
            """(;GC[a:b])"""
        )

        checkComposeValue(
            ValueTypeToken("a:b", TextSpan(5, 4)),
            ValueTypeToken("c:d", TextSpan(10, 4)),
            """(;GC[a\:b:c\:d])"""
        )

        checkComposeValue(
            ValueTypeToken("a\\]", TextSpan(5, 5)),
            ValueTypeToken("b\\]", TextSpan(11, 5)),
            """(;GC[a\\\]:b\\\]])"""
        )
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

    private fun parseAndGetFirstPropertyValue(input: String): PropertyValueType? {
        val root = SgfParser.parse(input)
        return root.gameTree.single().nodes.single().properties.single().value.single().valueType
    }

    private fun checkTokens(expectedToken: SgfToken, actualToken: SgfToken) {
        assertEquals(expectedToken::class, actualToken::class)
        assertEquals(expectedToken.value, actualToken.value)
        assertEquals(expectedToken.textSpan, actualToken.textSpan)
    }
}