package org.dots.game

import org.dots.game.core.InitialPositionType
import org.dots.game.core.MoveInfo
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.core.generateDefaultInitialPositions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InitialPositionTests {
    @Test
    fun crossOnMinimalField() {
        val initialPositions = InitialPositionType.Cross.generateDefaultInitialPositions(2, 2)!!
        initialPositions.checkCross(1, 1)
    }

    @Test
    fun crossOnEvenField() {
        val initialPositions = InitialPositionType.Cross.generateDefaultInitialPositions(8, 8)!!
        initialPositions.checkCross(4, 4)
    }

    @Test
    fun crossOnOddField() {
        val initialPositions = InitialPositionType.Cross.generateDefaultInitialPositions(9, 9)!!
        initialPositions.checkCross(4, 4)
    }

    @Test
    fun crossDoesntFitField() {
        assertNull(InitialPositionType.Cross.generateDefaultInitialPositions(1, 1))
    }

    @Test
    fun singlePositionGeneration() {
        assertNull(InitialPositionType.Single.generateDefaultInitialPositions(0, 0))
        assertEquals(MoveInfo(Position(0, 0), Player.First), InitialPositionType.Single.generateDefaultInitialPositions(1, 1)!!.single())
        assertEquals(MoveInfo(Position(1, 1), Player.First), InitialPositionType.Single.generateDefaultInitialPositions(2, 2)!!.single())
    }

    @Test
    fun doubleCrossGeneration() {
        assertNull(InitialPositionType.DoubleCross.generateDefaultInitialPositions(3, 2))

        val initialPositionsForMinSize = InitialPositionType.DoubleCross.generateDefaultInitialPositions(4, 2)!!
        initialPositionsForMinSize.take(4).checkCross(1, 1)
        initialPositionsForMinSize.drop(4).checkCross(3, 1, startPlayer = Player.Second)

        val initialPositionsForOddWidth = InitialPositionType.DoubleCross.generateDefaultInitialPositions(5, 2)!!
        initialPositionsForOddWidth.take(4).checkCross(1, 1)
        initialPositionsForOddWidth.drop(4).checkCross(3, 1, startPlayer = Player.Second)
    }

    @Test
    fun initialPositionTypeRecognition() {
        checkRecognition(InitialPositionType.Empty)

        checkRecognition(InitialPositionType.Single, MoveInfo(Position(19, 19), Player.First))
        checkRecognition(InitialPositionType.Single, MoveInfo(Position(19, 19), Player.Second))

        checkRecognition(InitialPositionType.Cross,
            MoveInfo(Position(19, 19), Player.First),
            MoveInfo(Position(20, 19), Player.Second),
            MoveInfo(Position(20, 20), Player.First),
            MoveInfo(Position(19, 20), Player.Second),
        )

        checkRecognition(InitialPositionType.Cross,
            MoveInfo(Position(19, 19), Player.First),
            MoveInfo(Position(20, 20), Player.First),
            MoveInfo(Position(20, 19), Player.Second),
            MoveInfo(Position(19, 20), Player.Second),
        )

        checkRecognition(InitialPositionType.Cross,
            MoveInfo(Position(19, 19), Player.Second),
            MoveInfo(Position(20, 19), Player.First),
            MoveInfo(Position(20, 20), Player.Second),
            MoveInfo(Position(19, 20), Player.First),
        )

        checkRecognition(InitialPositionType.Custom,
            MoveInfo(Position(19, 19), Player.First),
            MoveInfo(Position(20, 19), Player.Second),
            MoveInfo(Position(20, 20), Player.First),
            MoveInfo(Position(19, 20), Player.First),
        )

        checkRecognition(InitialPositionType.Custom,
            MoveInfo(Position(19, 19), Player.First),
            MoveInfo(Position(20, 19), Player.Second),
            MoveInfo(Position(21, 21), Player.First),
            MoveInfo(Position(19, 20), Player.Second),
        )

        checkRecognition(InitialPositionType.DoubleCross,
            MoveInfo(Position(19, 19), Player.First),
            MoveInfo(Position(20, 19), Player.Second),
            MoveInfo(Position(20, 20), Player.First),
            MoveInfo(Position(19, 20), Player.Second),

            MoveInfo(Position(21, 19), Player.Second),
            MoveInfo(Position(22, 19), Player.First),
            MoveInfo(Position(22, 20), Player.Second),
            MoveInfo(Position(21, 20), Player.First),
        )
    }

    private fun checkRecognition(expectedInitialPositionType: InitialPositionType, vararg actualMoveInfos: MoveInfo) {
        assertEquals(expectedInitialPositionType, Rules(initialMoves = actualMoveInfos.toList()).initialPositionType)
    }

    private fun List<MoveInfo>.checkCross(x: Int, y: Int, startPlayer: Player = Player.First) {
        assertEquals(4, this.size)
        val oppositePlayer = startPlayer.opposite()
        assertEquals(MoveInfo(Position(x, y), startPlayer), this[0])
        assertEquals(MoveInfo(Position(x + 1, y), oppositePlayer), this[1])
        assertEquals(MoveInfo(Position(x + 1, y + 1), startPlayer), this[2])
        assertEquals(MoveInfo(Position(x, y + 1), oppositePlayer), this[3])
    }
}