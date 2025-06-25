package org.dots.game.field.features

import org.dots.game.core.features.getOneMoveCapturingAndBasePositions
import org.dots.game.dump.FieldParser
import org.junit.jupiter.api.assertAll

import kotlin.test.Test

class FieldOneMovePositionsTests {
    @Test
    fun twoCapturing() {
        checkOneMoveCapturingAndSurroundPositions(
            fieldData = """
. * . . . + .
* + * . + * +
. . . . . . .
""",
            expectedCapturingPositionsData = """
. . . . . . .
. . . . . . .
. * . . . + .
""",
            expectedSurroundingPositionsData = """
.  .  .  .  .  .  .
.  *  .  .  .  +  .
.  .  .  .  .  .  .
""",
        )
    }

    @Test
    fun twoBases() {
        checkOneMoveCapturingAndSurroundPositions(
            fieldData = """
. * . . . + .
* . * . + . +
. . . . . . .
""",
            expectedCapturingPositionsData = null,
            expectedSurroundingPositionsData = """
. . . . . . .
. * . . . + .
. . . . . . .
"""
        )
    }

    @Test
    fun emptyBasePosition() {
        checkOneMoveCapturingAndSurroundPositions(
            fieldData = """
. * .
* . *
. * .
""",
            expectedCapturingPositionsData = null,
            expectedSurroundingPositionsData = """
. . .
. * .
. . .
"""
        )
    }

    @Test
    fun emptyBasePosition2() {
        checkOneMoveCapturingAndSurroundPositions(
            fieldData = """
. + .
+ . +
. + .
""",
            expectedCapturingPositionsData = null,
            expectedSurroundingPositionsData = """
. . .
. + .
. . .
"""
        )
    }

    @Test
    fun emptyBaseWithCapturing() {
        checkOneMoveCapturingAndSurroundPositions(
            fieldData = """
. * .
* + *
+ . +
. + .
""",
            expectedCapturingPositionsData = """
. . .
. . .
. * .
. . .
""",
            expectedSurroundingPositionsData = """
.  .  .
.  *  .
.  +  .
.  .  .
""",
        )
    }

    @Test
    fun twoCapturingOnTheSamePosition() {
        checkOneMoveCapturingAndSurroundPositions(
            fieldData = """
. * .
* + *
. . .
+ * +
. + .
""",
            expectedCapturingPositionsData = """
. .  .
. .  .
. *+ .
. .  .
. .  .
""",
            expectedSurroundingPositionsData = """
.  .  .
.  *  .
.  .  .
.  +  .
.  .  .
""",
        )
    }

    @Test
    fun twoBasesOnTheSamePosition() {
        checkOneMoveCapturingAndSurroundPositions(
            fieldData = """
. * * * .
* . + . *
* + . + *
* . . . *
. * . * .
""",
            expectedCapturingPositionsData = """
.  .  .  .  .
.  .  .  .  .
.  .  .  .  .
.  .  *  .  .
.  .  *  .  .
""",
            expectedSurroundingPositionsData = """
.  .  .  .  .
.  *  *  *  .
.  *  *+ *  .
.  *  *  *  .
.  .  .  .  .
""".trimIndent(),
        )
    }

    @Test
    fun complexExampleWithMultipleStates() {
        checkOneMoveCapturingAndSurroundPositions(fieldData = """
. + + * * .
+ . * + . *
+ * . + * .
+ * . + * .
. + . * . .
""",
            expectedCapturingPositionsData = """
.  .  .  .  .  .
.  .  .  .  .  .
.  .  .  .  .  .
.  .  *+ .  .  .
.  .  *+ .  .  .
""",
            expectedSurroundingPositionsData = """
.  .  .  .  .  .
.  +  +  *  *  .
.  +  *+ *  .  .
.  +  *+ *  .  .
.  .  .  .  .  .
""")
    }

    private fun checkOneMoveCapturingAndSurroundPositions(
        fieldData: String,
        expectedCapturingPositionsData: String? = null,
        expectedSurroundingPositionsData: String? = null,
    ) {
        val field = FieldParser.parseAndConvertWithNoInitialMoves(fieldData)
        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        val capturingFailure = checkFeatures(
            field,
            expectedCapturingPositionsData,
            capturingPositions,
            "Different capturing positions"
        )
        val surroundingFailure = checkFeatures(
            field,
            expectedSurroundingPositionsData,
            basePositions,
            "Different surrounding positions"
        )

        assertAll(listOfNotNull(capturingFailure, surroundingFailure))
    }
}