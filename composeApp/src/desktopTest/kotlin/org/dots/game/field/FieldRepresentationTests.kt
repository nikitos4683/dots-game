package org.dots.game.field

import org.dots.game.core.Field
import org.dots.game.core.Position
import org.dots.game.core.Rules
import kotlin.test.Test
import kotlin.test.assertEquals

class FieldRepresentationTests : FieldTests() {
    val sampleField = initialize("""
            .   *3  *4  *5 .  * * *
            *14 +1  .   .  *6 . . . *
            *13 .   *0  .  *7 . *2 . *
            *12 .   .   .  *8 . . . *
            .   *11 *10 *9 .  * * *
        """.trimIndent())

    @Test
    fun borders() {
        val field = Field(Rules(1, 1))
        field.makeMove(Position(1, 1))
        assertEquals("""
            ┌ ─ ┐
            │ * │
            └ ─ ┘
        """.trimIndent(), field.dump(printNumbers = false, padding = 1, printCoordinates = false, debugInfo = false))
    }

    @Test
    fun maxPadding() {
        val field = Field(Rules(5, 5))
        field.makeMove(Position(3, 3))
        assertEquals(
            """
                ┌ ─ ─ ─ ─ ─ ┐
                │ . . . . . │
                │ . . . . . │
                │ . . * . . │
                │ . . . . . │
                │ . . . . . │
                └ ─ ─ ─ ─ ─ ┘
            """.trimIndent(),
            field.dump(printNumbers = false, padding = Int.MAX_VALUE, printCoordinates = false, debugInfo = false)
        )
    }

    @Test
    fun coordinates() {
        assertEquals(
            """
            \  0  1  2  3  4  5  6  7  8  9  10
            0  ┌  ─  ─  ─  ─  ─  ─  ─  ─  ─  ┐
            1  │  .  *  *  *  .  *  *  *  .  │
            2  │  *  +  .  .  *  .  .  .  *  │
            3  │  *  .  *  .  *  .  *  .  *  │
            4  │  *  .  .  .  *  .  .  .  *  │
            5  │  .  *  *  *  .  *  *  *  .  │
            6  └  ─  ─  ─  ─  ─  ─  ─  ─  ─  ┘
        """.trimIndent(),
            sampleField.dump(printNumbers = false, padding = 1, printCoordinates = true, debugInfo = false)
        )
    }

    @Test
    fun numbers() {
        assertEquals(
            """
            .   *3  *4  *5  .   *15 *16 *17 .
            *14 +1  .   .   *6  .   .   .   *18
            *13 .   *0  .   *7  .   *2  .   *19
            *12 .   .   .   *8  .   .   .   *20
            .   *11 *10 *9  .   *21 *22 *23 .
        """.trimIndent(),
            sampleField.dump(printNumbers = true, padding = 0, printCoordinates = false, debugInfo = false)
        )
    }

    @Test
    fun debugInfo() {
        assertEquals(
            """
                .  *  *  *  .  *  *  *  .
                *  *+ *^ *^ *  `* `* `* *
                *  *^ ** *^ *  `* *  `* *
                *  *^ *^ *^ *  `* `* `* *
                .  *  *  *  .  *  *  *  .
            """.trimIndent(),
            sampleField.dump(printNumbers = false, padding = 0, printCoordinates = false, debugInfo = true)
        )
    }
}