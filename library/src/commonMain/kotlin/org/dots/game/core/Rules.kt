package org.dots.game.core

import org.dots.game.core.InitialPositionType.Cross
import org.dots.game.core.InitialPositionType.Empty
import org.dots.game.core.InitialPositionType.Single
import org.dots.game.core.InitialPositionType.DoubleCross
import org.dots.game.core.InitialPositionType.Custom

class Rules(
    val width: Int = 39,
    val height: Int = 32,
    val captureByBorder: Boolean = false,
    val baseMode: BaseMode = BaseMode.AtLeastOneOpponentDot,
    val suicideAllowed: Boolean = true,
    val initialMoves: List<MoveInfo> = Cross.generateDefaultInitialPositions(width, height) ?: emptyList(),
) {
    companion object {
        val Standard = Rules()
    }

    /**
     * The recognizer doesn't consider crosses orientation and positions.
     */
    val initialPositionType: InitialPositionType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        when (initialMoves.size) {
            0 -> Empty
            1 -> Single
            4 -> {
                val sortedMoveInfos = initialMoves.sortedBy { it.position.squareDistanceToZero() }
                if (recognizeCross(sortedMoveInfos, 0))
                    Cross
                else
                    Custom
            }
            8 -> {
                val sortedMoveInfos = initialMoves.sortedBy { it.position.squareDistanceToZero() }
                if (recognizeCross(sortedMoveInfos, 0) && recognizeCross(sortedMoveInfos, 4))
                    DoubleCross
                else
                    Custom
            }
            else -> Custom
        }
    }

    private fun recognizeCross(sortedMoveInfos: List<MoveInfo>, startMoveInfoIndex: Int): Boolean {
        val firstMoveInfo = sortedMoveInfos.elementAtOrNull(startMoveInfoIndex) ?: return false
        val secondMoveInfo = sortedMoveInfos.elementAtOrNull(startMoveInfoIndex + 1) ?: return false
        val thirdMoveInfo = sortedMoveInfos.elementAtOrNull(startMoveInfoIndex + 2) ?: return false
        if (secondMoveInfo.position.squareDistanceTo(firstMoveInfo.position) == 1 &&
            thirdMoveInfo.position.squareDistanceTo(firstMoveInfo.position) == 1 &&
            secondMoveInfo.position != thirdMoveInfo.position &&
            secondMoveInfo.player == thirdMoveInfo.player &&
            secondMoveInfo.player != firstMoveInfo.player
        ) {
            val fourthMoveInfo = sortedMoveInfos.elementAtOrNull(startMoveInfoIndex + 3) ?: return false
            if (fourthMoveInfo.position.squareDistanceTo(firstMoveInfo.position) == 2 &&
                fourthMoveInfo.player == firstMoveInfo.player
            ) {
                return true
            }
        }
        return false
    }
}

enum class InitialPositionType {
    Empty,
    Single,
    Cross,
    DoubleCross,
    Custom;
}

enum class BaseMode {
    /**
     * The surrounding becomes captured if at least it captures at least one enemy's dot.
     * It doesn't imply inner holes since they could be encountered only in a case
     * when an inner empty base is surrounded by an outer inner base.
     * The case is very rare and generally in this case there is no sense to place dot inside such territories.
     * That's why inner base holes are not implied and surroundings are always created by a minimal territory
     * excluding inner holes.
     * Also, the dots game format is surrounding-based unlike go-format that is more filling-based
     * (it affects algorithms for searching surroundings).
     */
    AtLeastOneOpponentDot,

    /**
     * The surrounding becomes captured even without any dot inside.
     * Inner holes can't be created.
     */
    AnySurrounding,

    /**
     * Surrounding should not contain empty positions.
     * The mode makes the game more like Go.
     * Inner holes are possible because it removes game state contradictions and makes the game more attractive for
     * go players that used to play in this way.
     */
    AllOpponentDots;
}

fun InitialPositionType.generateDefaultInitialPositions(width: Int, height: Int): List<MoveInfo>? {
    when (this) {
        Empty -> {
            return emptyList()
        }
        Single -> {
            if (width < 1 || height < 1) return null

            return listOf(MoveInfo(Position(width / 2, height / 2), Player.First))
        }
        Cross -> {
            if (width < 2 || height < 2) return null

            return mutableListOf<MoveInfo>().apply { addCross(Position(width / 2, height / 2), Player.First) }
        }
        DoubleCross -> {
            if (width < 4 || height < 2) return null

            val middleX = width / 2 + 1
            val middleY = height / 2
            return mutableListOf<MoveInfo>().apply {
                addCross(Position(middleX - 2, middleY), Player.First)
                addCross(Position(middleX, middleY), Player.Second)
            }
        }
        else -> {
            return emptyList()
        }
    }
}

private fun MutableList<MoveInfo>.addCross(position: Position, startPlayer: Player) {
    val oppPlayer = startPlayer.opposite()
    add(MoveInfo(position, startPlayer))
    add(MoveInfo(Position(position.x + 1, position.y), oppPlayer))
    add(MoveInfo(Position(position.x + 1, position.y + 1), startPlayer))
    add(MoveInfo(Position(position.x, position.y + 1), oppPlayer))
}