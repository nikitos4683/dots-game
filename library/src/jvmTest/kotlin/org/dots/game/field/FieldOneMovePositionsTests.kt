package org.dots.game.field

import org.dots.game.core.EMPTY_POSITION
import org.dots.game.core.FIRST_PLAYER_MARKER
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.SECOND_PLAYER_MARKER
import org.dots.game.core.getOneMoveCapturingAndBasePositions
import org.dots.game.dump.FieldParser
import java.util.SortedMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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
            expectedSurroundingPositionsData = null,
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
    fun noBaseIfCapturing() {
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
            expectedSurroundingPositionsData = null,
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
            expectedSurroundingPositionsData = null,
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
.  *  .  *  .  
.  .  *+ .  .  
.  *  .  *  .  
.  .  .  .  .
""".trimIndent(),
        )
    }

    private fun checkOneMoveCapturingAndSurroundPositions(
        fieldData: String,
        expectedCapturingPositionsData: String? = null,
        expectedSurroundingPositionsData: String? = null,
    ) {
        val field = FieldParser.parseAndConvertWithNoInitialMoves(fieldData)
        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        fun checkMoves(expectedPositionsData: String?, actualPositionsMap: Map<Position, Player>, capturing: Boolean) {
            val expectedPositions: SortedMap<Position, Player> = if (expectedPositionsData != null) {
                val (width, height, expectedLightMoves) = FieldParser.parse(expectedPositionsData)
                assertEquals(field.width, width)
                assertEquals(field.height, height)
                expectedLightMoves.map { it.value }.associate { it.position to it.player }.toSortedMap()
            } else {
                sortedMapOf()
            }

            val actualPositions: SortedMap<Position, Player> = actualPositionsMap.toSortedMap()

            if (expectedPositions != actualPositions) {
                fun SortedMap<Position, Player>.dump(): String {
                    return buildString {
                        for (y in 1..field.height) {
                            for (x in 1..field.width) {
                                append(
                                    when (this@dump[Position(x, y)]) {
                                        Player.First -> "$FIRST_PLAYER_MARKER "
                                        Player.Second -> "$SECOND_PLAYER_MARKER "
                                        Player.Both -> "$FIRST_PLAYER_MARKER$SECOND_PLAYER_MARKER"
                                        else -> "$EMPTY_POSITION "
                                    }
                                )
                                append(' ')
                            }
                            appendLine()
                        }
                    }
                }

                // Check string for a more convenient comparison
                assertEquals(
                    expectedPositions.dump(),
                    actualPositions.dump(),
                    "Different ${if (capturing) "capturing" else "surrounding" } positions")
                fail("Should not be here. Fix the comparison function")
            }
        }

        checkMoves(expectedCapturingPositionsData, capturingPositions, capturing = true)
        checkMoves(expectedSurroundingPositionsData, basePositions, capturing = false)
    }
}