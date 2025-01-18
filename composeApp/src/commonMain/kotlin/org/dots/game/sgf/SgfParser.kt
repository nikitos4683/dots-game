package org.dots.game.sgf

/**
 * Reference: https://homepages.cwi.nl/~aeb/go/misc/sgfnotes.html
 *
 * Grammar:
 *
 * ```
 * Collection = GameTree+
 * GameTree   = '(' Node+ GameTree* ')'
 * Node       = ';' Property*
 * Property   = UcLetter+ PropertyValue+
 * PropertyValue  = '[' PropertyValueType? ']'
 * PropertyValueType = ValueType composePart=(':' ValueType?)?
 * ValueType  = '~[:\\\]]+'
 * UcLetter   = 'A'..'Z'
 * ```
 */
class SgfParser private constructor(val text: CharSequence, val errorReporter: (SgfToken) -> Unit) {
    companion object {
        private const val IDENTIFIER_START: Char = 'A'
        private const val IDENTIFIER_END: Char = 'Z'
        private val notPropertyValueTypeChars: Set<Char> = setOf(':', ']')

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
            UnparsedText(text.substring(currentIndex), TextSpan(currentIndex, text.length - currentIndex)).also { it.reportIfError() }
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

        val childGameTrees = buildList {
            while (expect('(')) {
                add(parseGameTree())
            }
        }

        val rParen = RParenToken(tryMatchChar(')')).also { it.reportIfError() }

        return GameTree(lParen, nodes, childGameTrees, rParen, getCurrentTextSpan(initialIndex))
    }

    private fun parseNode(): Node {
        val initialIndex = currentIndex

        val semicolonToken = SemicolonToken(matchChar(';'))

        val properties = buildList {
            while (expectIdentifier()) {
                add(parseProperty())
            }
        }

        return Node(semicolonToken, properties, getCurrentTextSpan(initialIndex))
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

        val lSquareBracketToken = LSquareBracketToken(matchChar('['))

        val propertyValueType = if (expectPropertyValueType()) {
            parsePropertyValueType()
        } else {
            null
        }

        val rSquareBracketToken = RSquareBracketToken(tryMatchChar(']')).also { it.reportIfError() }

        return PropertyValue(lSquareBracketToken, propertyValueType, rSquareBracketToken, getCurrentTextSpan(initialIndex))
    }

    private fun parsePropertyValueType(): PropertyValueType {
        val initialIndex = currentIndex

        val propertyValueTypeToken = parsePropertyValueTypeToken()

        val propertyValueComposeType = if (expect(':')) {
            parsePropertyValueTypeComposePart()
        } else {
            null
        }

        return PropertyValueType(propertyValueTypeToken, propertyValueComposeType, getCurrentTextSpan(initialIndex))
    }

    private fun parsePropertyValueTypeComposePart(): PropertyValueComposeType {
        val initialIndex = currentIndex

        val colonToken = ColonToken(matchChar(':'))

        val valueTypeToken = parsePropertyValueTypeToken()

        return PropertyValueComposeType(colonToken, valueTypeToken, getCurrentTextSpan(initialIndex))
    }

    private fun parsePropertyValueTypeToken(): ValueTypeToken {
        val initialIndex = currentIndex

        val propertyValueTypeString = buildString {
            while (checkBounds()) {
                when (val currentChar = text[currentIndex]) {
                    ':', ']' -> break
                    '\\' -> {
                        currentIndex++
                        if (checkBounds()) {
                            append(text[currentIndex])
                            currentIndex++
                        }
                    }
                    else -> append(currentChar).also { currentIndex++ }
                }
            }
        }

        return ValueTypeToken(propertyValueTypeString, getCurrentTextSpan(initialIndex))
    }

    private fun expectIdentifier(): Boolean {
        return checkBounds() && text[currentIndex].checkIdentifierChar()
    }

    private fun Char.checkIdentifierChar(): Boolean = this >= IDENTIFIER_START && this <= IDENTIFIER_END

    private fun expectPropertyValueType(): Boolean {
        return checkBounds() && text[currentIndex] !in notPropertyValueTypeChars
    }

    private fun checkBounds(): Boolean = currentIndex >= 0 && currentIndex < text.length

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

    private fun expect(char: Char): Boolean {
        return checkBounds() && text[currentIndex] == char
    }

    private fun SgfToken.reportIfError() {
        if (isError) {
            errorReporter(this)
        }
    }

    private fun getCurrentTextSpan(initialIndex: Int): TextSpan = TextSpan(initialIndex, currentIndex - initialIndex)
}