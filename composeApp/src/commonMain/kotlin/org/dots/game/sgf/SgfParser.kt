package org.dots.game.sgf

/**
 * Reference: https://homepages.cwi.nl/~aeb/go/misc/sgf.html
 *
 * Grammar:
 *
 * ```
 * Collection = GameTree+
 * GameTree   = '(' Node+ GameTree* ')'
 * Node       = ';' Property*
 * Property   = UcLetter+ PropertyValue+
 * PropertyValue  = '[' PropertyValueType? ']'
 * PropertyValueType = '~[:\\\]]+'
 * UcLetter   = 'A'..'Z'
 * ```
 */
class SgfParser private constructor(val text: CharSequence, val errorReporter: (SgfToken) -> Unit) {
    companion object {
        fun parse(sgfText: String, errorReporter: (SgfToken) -> Unit = {}): SgfRoot {
            return SgfParser(sgfText, errorReporter).parse()
        }
    }

    private var currentIndex = 0

    private fun parse(): SgfRoot {
        val initialIndex = currentIndex
        val gameTrees = buildList {
            while (expect('(')) {
                add(parseGameTree())
            }
        }
        val unparsedText = if (currentIndex < text.length) {
            UnparsedText(
                text.substring(currentIndex),
                TextSpan(currentIndex, text.length - currentIndex)
            ).also { it.reportIfError() }
        } else {
            null
        }
        return SgfRoot(gameTrees, unparsedText, getCurrentTextSpan(initialIndex))
    }

    private fun parseGameTree(): GameTree {
        val initialIndex = currentIndex

        val lParen = LParenToken(matchChar('('))

        val nodes = buildList {
            while (expect(';')) {
                add(parseNode())
            }
        }

        val childrenGameTrees = buildList {
            while (expect('(')) {
                add(parseGameTree())
            }
        }

        val rParen = RParenToken(tryMatchChar(')')).also { it.reportIfError() }

        return GameTree(lParen, nodes, childrenGameTrees, rParen, getCurrentTextSpan(initialIndex))
    }

    private fun parseNode(): Node {
        val initialIndex = currentIndex

        val semicolon = SemicolonToken(matchChar(';'))

        val properties = buildList {
            while (checkBounds() && text[currentIndex].checkIdentifierChar()) {
                add(parseProperty())
            }
        }

        return Node(semicolon, properties, getCurrentTextSpan(initialIndex))
    }

    private fun parseProperty(): Property {
        val initialIndex = currentIndex

        do {
            if (text[currentIndex].checkIdentifierChar()) {
                currentIndex++
            } else {
                break
            }
        } while (checkBounds())

        val identifier = IdentifierToken(text.substring(initialIndex, currentIndex), getCurrentTextSpan(initialIndex))

        val values = buildList {
            while (expect('[')) {
                add(parsePropertyValue())
            }
        }

        return Property(identifier, values, getCurrentTextSpan(initialIndex))
    }

    private fun parsePropertyValue(): PropertyValue {
        val initialIndex = currentIndex

        val lSquareBracket = LSquareBracketToken(matchChar('['))

        val propertyValueToken = if (checkBounds() && text[currentIndex] != ']') {
            parsePropertyValueToken()
        } else {
            null
        }

        val rSquareBracket = RSquareBracketToken(tryMatchChar(']')).also { it.reportIfError() }

        return PropertyValue(lSquareBracket, propertyValueToken, rSquareBracket, getCurrentTextSpan(initialIndex))
    }

    private fun parsePropertyValueToken(): PropertyValueToken {
        val initialIndex = currentIndex

        val propertyValueString = buildString {
            do {
                when (val currentChar = text[currentIndex]) {
                    ']' -> break
                    '\\' -> {
                        currentIndex++
                        if (checkBounds()) {
                            append(text[currentIndex])
                            currentIndex++
                        }
                    }

                    else -> append(currentChar).also { currentIndex++ }
                }
            } while (checkBounds())
        }

        return PropertyValueToken(propertyValueString, getCurrentTextSpan(initialIndex))
    }

    private fun Char.checkIdentifierChar(): Boolean = this >= 'A' && this <= 'Z'

    private fun matchChar(char: Char): TextSpan {
        require(text[currentIndex] == char)
        return TextSpan(currentIndex, 1).also { currentIndex++ }
    }

    private fun tryMatchChar(char: Char): TextSpan {
        val initialIndex = currentIndex
        val length = if (expect(char)) {
            currentIndex++
            1
        } else {
            0
        }
        return TextSpan(initialIndex, length)
    }

    private fun expect(char: Char): Boolean = checkBounds() && text[currentIndex] == char

    private fun checkBounds(): Boolean = currentIndex >= 0 && currentIndex < text.length

    private fun SgfToken.reportIfError() {
        if (isError) {
            errorReporter(this)
        }
    }

    private fun getCurrentTextSpan(initialIndex: Int): TextSpan = TextSpan(initialIndex, currentIndex - initialIndex)
}