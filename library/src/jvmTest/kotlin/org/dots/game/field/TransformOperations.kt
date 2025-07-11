package org.dots.game.field

import DumpParameters
import org.dots.game.core.Field
import org.dots.game.core.TransformType
import org.dots.game.dump.FieldParser
import render
import kotlin.test.Test
import kotlin.test.assertEquals

class TransformOperations : FieldTests() {
    @Test
    fun testTransformation() {
        val originFieldData =
"""
.  .  .  .
.  *0 *1 .
*8 +7 .  *2
.  *4 *3 +5
.  *6 .  .
"""
        val originField = FieldParser.parseAndConvertWithNoInitialMoves(originFieldData)

        checkOperation(
            originField,
            """┌  ─  ─  ─  ─  ─  ┐
│  .  .  *8 .  .  │
│  *6 *4 +7 *0 .  │
│  .  *3 .  *1 .  │
│  .  +5 *2 .  .  │
└  ─  ─  ─  ─  ─  ┘""",
            TransformType.RotateCw90
        )

        checkOperation(
            originField,
            """┌  ─  ─  ─  ─  ┐
│  .  .  *6 .  │
│  +5 *3 *4 .  │
│  *2 .  +7 *8 │
│  .  *1 *0 .  │
│  .  .  .  .  │
└  ─  ─  ─  ─  ┘""",
            TransformType.Rotate180
        )

        checkOperation(
            originField,
            """┌  ─  ─  ─  ─  ─  ┐
│  .  .  *8 .  .  │
│  *6 *4 +7 *0 .  │
│  .  *3 .  *1 .  │
│  .  +5 *2 .  .  │
└  ─  ─  ─  ─  ─  ┘""",
            TransformType.RotateCw90
        )

        checkOperation(
            originField,
            """┌  ─  ─  ─  ─  ┐
│  .  .  .  .  │
│  .  *1 *0 .  │
│  *2 .  +7 *8 │
│  +5 *3 *4 .  │
│  .  .  *6 .  │
└  ─  ─  ─  ─  ┘""",
            TransformType.FlipHorizontal
        )

        checkOperation(
            originField,
            """┌  ─  ─  ─  ─  ┐
│  .  *6 .  .  │
│  .  *4 *3 +5 │
│  *8 +7 .  *2 │
│  .  *0 *1 .  │
│  .  .  .  .  │
└  ─  ─  ─  ─  ┘""",
            TransformType.FlipVertical
        )
    }

    private fun checkOperation(originField: Field, expectedData: String, transformType: TransformType) {
        val transformedField = originField.transform(transformType)
        assertEquals(expectedData, transformedField.render(DumpParameters(printCoordinates = false)))
        assertEquals(originField.initialMovesCount, transformedField.initialMovesCount)
        assertEquals(originField.player1Score, transformedField.player1Score)
        assertEquals(1, transformedField.player1Score)
        assertEquals(originField.player2Score, transformedField.player2Score)
        assertEquals(originField.gameResult, transformedField.gameResult)
        assertEquals(originField.numberOfLegalMoves, transformedField.numberOfLegalMoves)
    }
}