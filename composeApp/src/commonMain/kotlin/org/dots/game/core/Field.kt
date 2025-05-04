package org.dots.game.core

import render

class Field(val rules: Rules = Rules.Standard, onIncorrectInitialMove: (MoveInfo, Boolean, Int) -> Unit = { _, _, _ -> }) {
    companion object {
        const val OFFSET: Int = 1
        const val MAX_WIDTH = (1 shl Position.COORDINATE_BITS_COUNT) - 2
        const val MAX_HEIGHT = (1 shl Position.COORDINATE_BITS_COUNT) - 2

        fun checkWidth(value: Int): Boolean = value >= 0 && value < MAX_WIDTH
        fun checkHeight(value: Int): Boolean = value >= 0 && value < MAX_HEIGHT
    }

    val width: Int = rules.width
    val height: Int = rules.height
    val realWidth: Int = width + OFFSET * 2
    val realHeight: Int = height + OFFSET * 2
    val initialMovesCount: Int

    // Initialize field as a primitive int array with expanded borders to get rid of extra range checks and boxing
    private val dots: IntArray = IntArray(realWidth * realHeight) { DotState.Empty.value }

    private val moveResults = mutableListOf<MoveResult>()

    init {
        require(checkWidth(width) && checkHeight(height))

        if (rules.captureByBorder) {
            for (x in 0 until realWidth) {
                Position(x, 0).setState(DotState.Border)
                Position(x, realHeight - 1).setState(DotState.Border)
            }
            for (y in 0 until realHeight) {
                Position(0, y).setState(DotState.Border)
                Position(realWidth - 1, y).setState(DotState.Border)
            }
        }

        for (moveInfo in rules.initialMoves) {
            val position = moveInfo.position
            if (!checkPositionWithinBounds(position)) {
                onIncorrectInitialMove(moveInfo, false, currentMoveNumber)
                continue
            }

            if (!checkValidMove(position, moveInfo.player)) {
                onIncorrectInitialMove(moveInfo, true, currentMoveNumber)
                continue
            }

            makeMoveUnsafe(position, moveInfo.player)
        }

        initialMovesCount = moveResults.size
    }

    val moveSequence: List<MoveResult> = moveResults

    val currentMoveNumber: Int
        get() = moveResults.size - initialMovesCount

    val lastMove: MoveResult?
        get() = moveResults.lastOrNull()

    var player1Score: Int = 0
        private set

    var player2Score: Int = 0
        private set

    fun getCurrentPlayer(): Player = moveResults.lastOrNull()?.player?.opposite() ?: Player.First

    fun getScoreDiff(): Int {
        return if (getCurrentPlayer() == Player.First) { player1Score - player2Score } else { player2Score - player1Score }
    }

    /**
     * Valid real positions starts from `1`, but not from `0`.
     * `0` is reserved for the initial position (cross, empty or other).
     */
    fun makeMove(position: Position, player: Player? = null): MoveResult? {
        return if (checkPositionWithinBounds(position) && checkValidMove(position, player))
            makeMoveUnsafe(position, player)
        else
            null
    }

    fun checkPositionWithinBounds(position: Position): Boolean {
        val (x, y) = position
        return x >= OFFSET && x < width + OFFSET && y >= OFFSET && y < height + OFFSET
    }

    fun checkValidMove(position: Position, player: Player?): Boolean {
        return !position.getState().checkPlacedOrTerritory() && (
                rules.suicideAllowed ||
                        makeMoveUnsafe(position, player).let { moveResult ->
                            if (moveResult != null) {
                                unmakeMove()
                                true
                            } else {
                                false
                            }
                        }
                )
    }

    fun unmakeAllMoves() {
        while (currentMoveNumber > 0) {
            unmakeMove()
        }
    }

    fun unmakeMove(): MoveResult? {
        if (currentMoveNumber == 0) return null

        val moveResult = moveResults.removeLast()

        for (base in moveResult.bases) {
            for ((position, previousState) in base.previousStates) {
                position.setState(previousState)
            }
            updateScoreCount(base.player, base.playerDiff, base.oppositePlayerDiff, rollback = true)
        }

        moveResult.position.setState(moveResult.previousState)
        for ((position, previousState) in moveResult.extraPreviousStates) {
            position.setState(previousState)
        }

        return moveResult
    }

    internal fun makeMoveUnsafe(position: Position, player: Player? = null): MoveResult? {
        val currentPlayer = player ?: getCurrentPlayer()

        val originalState = position.getState()

        val state = currentPlayer.createPlacedState()
        position.setState(state)

        val bases = tryCapture(position, state, emptyBaseCapturing = false)

        val oppositePlayer = currentPlayer.opposite()
        val resultBases: List<Base>
        val extraPreviousStates: Map<Position, DotState>

        // Handle possible suicidal moves.
        if (bases.isEmpty()) {
            resultBases = if (rules.baseMode != BaseMode.AllOpponentDots) {
                if (originalState.checkWithinEmptyTerritory(currentPlayer.opposite())) {
                    if (rules.suicideAllowed) {
                        // Check capturing by the opposite player
                        val oppositePlayerPlaced = currentPlayer.opposite().createPlacedState()
                        val oppositeBase = captureWhenEmptyTerritoryBecomesRealBase(position, oppositePlayerPlaced)
                        listOf(oppositeBase)
                    } else {
                        position.setState(originalState)
                        return null
                    }
                } else {
                    bases
                }
            } else {
                tryGetBaseForAllOpponentDotsMode(
                    position,
                    oppositePlayer.createPlacedState(),
                    capturingByOppositePlayer = true
                )?.let { (base, suicidalMove) ->
                    if (suicidalMove) {
                        // Rollback state in case of a suicidal move
                        position.setState(originalState)
                        return null
                    } else {
                        base?.let { listOf(it) } ?: bases
                    }
                } ?: bases
            }
            extraPreviousStates = emptyMap() // Not used in `AllOpponentDots` mode
        } else {
            resultBases = bases
            extraPreviousStates = if (rules.baseMode != BaseMode.AllOpponentDots &&
                originalState.checkWithinEmptyTerritory(currentPlayer.opposite())
            ) {
                // Invalidate empty territory of the opposite player in case of capturing that is more prioritized
                // It makes the positions legal for further game.
                invalidateEmptyTerritory(position)
            } else {
                emptyMap()
            }
        }

        return MoveResult(
            position,
            currentPlayer,
            currentMoveNumber + 1,
            originalState,
            extraPreviousStates,
            resultBases,
        ).also { moveResults.add(it) }
    }

    private fun captureWhenEmptyTerritoryBecomesRealBase(initialPosition: Position, oppositePlayerPlaced: DotState): Base {
        var (x, y) = initialPosition

        // Searching for an opponent dot that makes a closure that contains the `initialPosition`.
        // The closure always exists, otherwise there is an error in previous calculations.
        while (x > 0) {
            x--
            val position = Position(x, y)

            // Try to peek an active opposite player dot
            if (!position.getState().checkActive(oppositePlayerPlaced)) continue

            val oppositePlayerBased = tryCapture(position, oppositePlayerPlaced, emptyBaseCapturing = true)
            // The found base always should be real and include the `initialPosition`
            return oppositePlayerBased.firstOrNull { it.isReal } ?: continue
        }

        error("Enemy's empty territory should be enclosed by an outer closure")
    }

    private fun tryCapture(position: Position, playerPlaced: DotState, emptyBaseCapturing: Boolean): List<Base> {
        return if (rules.baseMode != BaseMode.AllOpponentDots) {
            val unconnectedPositions = getUnconnectedPositions(position, playerPlaced)

            // Optimization: in a regular case it should be at least 2 connection dots, otherwise there is no surrounding.
            // However, in the case of empty territory, the connection might be singular since the base is already built.
            val minNumberOfConnections = if (emptyBaseCapturing) 1 else 2
            if (unconnectedPositions.size < minNumberOfConnections) return emptyList()

            val closures = buildList {
                for (unconnectedPosition in unconnectedPositions) {
                    tryGetCounterCounterClockwiseClosure(position, unconnectedPosition, playerPlaced)?.let { add(it) }
                }
            }

            val resultClosures = if (rules.captureByBorder) {
                // Ignore bound closure with max square
                val topLeftBoundClosureWithMaxSquare = calculateTopLeftBoundClosureWithMaxSquare(closures)
                closures.filter { it != topLeftBoundClosureWithMaxSquare }
            } else {
                closures
            }

            resultClosures.map {
                buildBase(
                    it.closure,
                    playerPlaced,
                )
            }
        } else {
            val oppositeAdjacentPositions = getOppositeAdjacentPositions(position, playerPlaced)

            buildList {
                for (oppositeAdjacentPosition in oppositeAdjacentPositions) {
                    if (any { it.previousStates.containsKey(oppositeAdjacentPosition) }) continue // Ignore already processed bases
                    tryGetBaseForAllOpponentDotsMode(oppositeAdjacentPosition, playerPlaced, capturingByOppositePlayer = false)?.let {
                        require(!it.suicidalMove)
                        add(it.base!!)
                    }
                }
            }
        }
    }

    private fun calculateTopLeftBoundClosureWithMaxSquare(closures: List<ClosureData>): ClosureData? {
        var result: ClosureData? = null
        for (closure in closures) {
            if (!closure.containsBorder) continue

            if (result == null) {
                result = closure
                continue
            }

            val squareDiff = closure.square - result.square
            if (squareDiff > 0) {
                result = closure
                continue
            }

            if (squareDiff < 0) continue

            // Very rare case when a placed dot split the entire field into two equal parts
            // But we anyway need to formalize it
            val minPositionDiff = closure.closure.minOf { it.squareDistanceToZero() } -
                    result.closure.minOf { it.squareDistanceToZero() }
            if (minPositionDiff > 0) {
                result = closure
                continue
            }

            if (minPositionDiff < 0) continue

            // The rarer case when a placed dot split the entire field by diagonal chain
            // that starts from top-left corner to the bottom-right corner
            val diffX = closure.closure.minOf { it.x } - result.closure.minOf { it.x }
            if (diffX > 0) {
                result = closure
            }

            // Impossible case
            require(diffX != 0)
        }
        return result
    }

    /**
     * Returns the number of connections that should be checked on surrounding: `1` for a singular dot, `4` for the following case:
     *
     * ```
     * + . +
     * . o .
     * + . +
     * ```
     *
     * Where `o` is the checking @param [position].
     */
    private fun getUnconnectedPositions(position: Position, playerPlaced: DotState): List<Position> {
        val startPositions = mutableListOf<Position>()

        val (x, y) = position
        val xMinusOneY = Position(x - 1, y)
        val xYMinusOne = Position(x, y - 1)
        val xPlusOneY = Position(x + 1, y)
        val xYPlusOne = Position(x, y + 1)
        val xMinusOneYState = xMinusOneY.getState()
        val xYMinusOneState = xYMinusOne.getState()
        val xPlusOneYState = xPlusOneY.getState()
        val xYPlusOneState = xYPlusOne.getState()

        fun checkAndAdd(
            checkState: DotState,
            addPosition1: Position,
            addPosition2: Position,
            addPosition2State: DotState
        ) {
            if (!checkState.checkActive(playerPlaced)) {
                if (addPosition1.getState().checkActive(playerPlaced)) {
                    startPositions.add(addPosition1)
                } else if (addPosition2State.checkActive(playerPlaced)) {
                    startPositions.add(addPosition2)
                }
            }
        }

        checkAndAdd(xPlusOneYState, Position(x + 1, y + 1), xYPlusOne, xYPlusOneState)
        checkAndAdd(xYPlusOneState, Position(x - 1, y + 1), xMinusOneY, xMinusOneYState)
        checkAndAdd(xMinusOneYState, Position(x - 1, y - 1), xYMinusOne, xYMinusOneState)
        checkAndAdd(xYMinusOneState, Position(x + 1, y - 1), xPlusOneY, xPlusOneYState)

        return startPositions
    }

    private fun getOppositeAdjacentPositions(position: Position, playerPlaced: DotState): List<Position> {
        val player = playerPlaced.getPlacedPlayer()
        val oppositePlaced = player.opposite().createPlacedState()

        val (x, y) = position

        return buildList {
            fun Position.addIfActive() {
                if (getState().checkPlaced(oppositePlaced)) add(this)
            }

            Position(x, y - 1).addIfActive()
            Position(x + 1, y).addIfActive()
            Position(x, y + 1).addIfActive()
            Position(x - 1, y).addIfActive()
        }
    }

    private fun buildBase(
        closurePositions: List<Position>,
        playerPlaced: DotState,
    ): Base {
        val territoryPositions = getPositionsWithinClosure(closurePositions, playerPlaced)
        return updateStatesAndScores(closurePositions, territoryPositions, playerPlaced)
    }

    private data class ClosureData(
        val square: Int,
        val closure: List<Position>,
        val containsBorder: Boolean,
    )

    private fun tryGetCounterCounterClockwiseClosure(
        initialPosition: Position,
        startPosition: Position,
        playerPlaced: DotState
    ): ClosureData? {
        val closurePositions = mutableListOf(initialPosition, startPosition)
        var square = initialPosition.getSquare(startPosition)

        var currentPosition: Position = startPosition
        var nextPosition: Position = initialPosition

        var containsBorder = false

        loop@ do {
            val clockwiseWalkCompleted = currentPosition.clockwiseBigJumpWalk(nextPosition) {
                val state = it.getState()
                val isActive = state.checkActive(playerPlaced)

                if (isActive) {
                    square += currentPosition.getSquare(it)

                    if (it == initialPosition) {
                        break@loop
                    }

                    if (state.checkBorder()) {
                        containsBorder = true
                    }

                    closurePositions.add(it)
                    nextPosition = currentPosition
                    currentPosition = it
                    return@clockwiseBigJumpWalk false
                }
                return@clockwiseBigJumpWalk true
            }
            if (clockwiseWalkCompleted) {
                return null
            }
        } while (true)

        return if (square > 0) ClosureData(square, closurePositions, containsBorder) else null
    }

    private fun getPositionsWithinClosure(closurePositions: List<Position>, playerPlaced: DotState): Set<Position> {
        val walkStack = mutableListOf<Position>()
        val closurePositionsSet = closurePositions.toSet()
        val territoryPositions = mutableSetOf<Position>()
        val player = playerPlaced.getPlacedPlayer()
        val playerTerritory = player.createTerritoryState()

        fun Position.checkAndAdd() {
            val state = getState()
            if (state.checkTerritory(playerTerritory)) return // Ignore already captured territory

            if (this in closurePositionsSet) return
            if (!territoryPositions.add(this)) return

            walkStack.add(this)
        }

        val territoryFirstPosition = closurePositions[1].getNextClockwisePosition(closurePositions[0])
        territoryFirstPosition.checkAndAdd()

        while (walkStack.isNotEmpty()) {
            val currentPosition = walkStack.removeLast()

            val (x, y) = currentPosition
            Position(x, y - 1).checkAndAdd()
            Position(x + 1, y).checkAndAdd()
            Position(x, y + 1).checkAndAdd()
            Position(x - 1, y).checkAndAdd()
        }

        return territoryPositions
    }

    private fun tryGetBaseForAllOpponentDotsMode(
        territoryFirstPosition: Position,
        playerPlaced: DotState,
        capturingByOppositePlayer: Boolean,
    ): BaseWithRollbackInfo? {
        require(rules.baseMode == BaseMode.AllOpponentDots)

        val walkStack = mutableListOf<Position>()
        val territoryPositions = mutableSetOf<Position>()
        val closurePositions = mutableSetOf<Position>()
        var currentPlayerDiff = 0
        var oppositePlayerDiff = 0
        val player = playerPlaced.getPlacedPlayer()
        val playerTerritory = player.createTerritoryState()
        val oppositePlayer = player.opposite()
        val oppositePlayerTerritory = oppositePlayer.createTerritoryState()

        fun Position.checkAndAdd(): Boolean {
            val state = getState()

            if (state.checkBorder()) {
                return if (!rules.captureByBorder) {
                    false
                } else {
                    closurePositions.add(this)
                    true
                }
            } else if (!state.checkPlaced()) {
                return false // early return if encounter an empty position
            }

            if (territoryPositions.contains(this)) return true

            if (state.checkPlaced(playerPlaced)) {
                if (state.checkTerritory(oppositePlayerTerritory)) {
                    oppositePlayerDiff--
                } else {
                    closurePositions.add(this)
                    return true
                }
            } else { // Opposite player placed
                if (!state.checkTerritory(playerTerritory)) {
                    currentPlayerDiff++
                }
            }

            territoryPositions.add(this)
            walkStack.add(this)

            return true
        }

        if (!territoryFirstPosition.checkAndAdd()) return null

        while (walkStack.isNotEmpty()) {
            val currentPosition = walkStack.removeLast()

            val (x, y) = currentPosition
            if (!Position(x, y - 1).checkAndAdd()) return null
            if (!Position(x + 1, y).checkAndAdd()) return null
            if (!Position(x, y + 1).checkAndAdd()) return null
            if (!Position(x - 1, y).checkAndAdd()) return null
        }

        val base: Base?
        val suicidalMove: Boolean

        if (capturingByOppositePlayer && !rules.suicideAllowed) {
            base = null
            suicidalMove = true
        } else {
            base = createBaseAndUpdateStates(
                player,
                currentPlayerDiff,
                oppositePlayerDiff,
                closurePositions.toList(),
                territoryPositions,
                isReal = true,
            )
            suicidalMove = false
        }

        return BaseWithRollbackInfo(base, suicidalMove)
    }

    private data class BaseWithRollbackInfo(val base: Base?, val suicidalMove: Boolean)

    private fun updateStatesAndScores(
        closurePositions: List<Position>,
        territoryPositions: Set<Position>,
        playerPlaced: DotState,
    ): Base {
        var currentPlayerDiff = 0
        var oppositePlayerDiff = 0

        val player = playerPlaced.getPlacedPlayer()
        val playerTerritory = player.createTerritoryState()
        val oppositePlayer = player.opposite()
        val oppositePlayerPlaced = oppositePlayer.createPlacedState()
        val oppositePlayerTerritory = oppositePlayer.createTerritoryState()

        fun DotState.updateScoreDiff() {
            if (checkPlaced(oppositePlayerPlaced) && !checkTerritory(playerTerritory)) {
                // No diff for already captured territory (it's actual for empty bases)
                currentPlayerDiff++
            } else if (checkPlaced(playerPlaced) && checkTerritory(oppositePlayerTerritory)) {
                // No diff for the territory of the current player
                oppositePlayerDiff--
            }
        }

        for (territoryPosition in territoryPositions) {
            territoryPosition.getState().updateScoreDiff()
        }

        val isReal = when (rules.baseMode) {
            BaseMode.AtLeastOneOpponentDot -> currentPlayerDiff > 0
            BaseMode.AnySurrounding -> true
            BaseMode.AllOpponentDots -> error("The mode ${BaseMode.AllOpponentDots.name} is handled by ${::tryGetBaseForAllOpponentDotsMode.name}")
        }

        return createBaseAndUpdateStates(
            player,
            currentPlayerDiff,
            oppositePlayerDiff,
            closurePositions,
            territoryPositions,
            isReal,
        )
    }

    private fun createBaseAndUpdateStates(
        player: Player,
        currentPlayerDiff: Int,
        oppositePlayerDiff: Int,
        closurePositions: List<Position>,
        territoryPositions: Set<Position>,
        isReal: Boolean,
    ): Base {
        val previousStates = mutableMapOf<Position, DotState>()

        val base = Base(
            player,
            currentPlayerDiff,
            oppositePlayerDiff,
            closurePositions,
            previousStates,
            isReal,
        )

        updateScoreCount(player, currentPlayerDiff, oppositePlayerDiff, rollback = false)

        val playerTerritory = player.createTerritoryState()
        val playerEmptyTerritory = player.createEmptyTerritoryState()

        for (territoryPosition in territoryPositions) {
            val territoryPositionState = territoryPosition.getState()

            fun savePreviousStateAndSetNew(newState: DotState) {
                previousStates[territoryPosition] = territoryPositionState
                territoryPosition.setState(newState)
            }

            if (!isReal) {
                if (!territoryPositionState.checkPlacedOrTerritory()) {
                    savePreviousStateAndSetNew(playerEmptyTerritory)
                }
            } else {
                savePreviousStateAndSetNew(territoryPositionState.setTerritory(playerTerritory))
            }
        }

        return base
    }

    private fun updateScoreCount(player: Player, currentPlayerDiff: Int, oppositePlayerDiff: Int, rollback: Boolean) {
        val multiplier = if (rollback) -1 else 1
        if (player == Player.First) {
            player1Score += currentPlayerDiff * multiplier
            player2Score += oppositePlayerDiff * multiplier
        } else {
            player1Score += oppositePlayerDiff * multiplier
            player2Score += currentPlayerDiff * multiplier
        }
    }

    /**
     * Invalidates states of an empty base that becomes broken.
     */
    private fun invalidateEmptyTerritory(position: Position): Map<Position, DotState> {
        val walkStack = mutableListOf<Position>().also { it.add(position) }
        val emptyTerritoryPositions = mutableMapOf<Position, DotState>()

        fun Position.checkAndAdd() {
            val state = getState()
            if (!state.checkWithinEmptyTerritory()) return
            val existingTerritoryPosition = emptyTerritoryPositions[this]
            if (existingTerritoryPosition == null) {
                emptyTerritoryPositions[this] = state
                setState(DotState.Empty)
            } else {
                return
            }

            walkStack.add(this)
        }

        while (walkStack.isNotEmpty()) {
            val currentPosition = walkStack.removeLast()

            val (x, y) = currentPosition
            Position(x, y - 1).checkAndAdd()
            Position(x + 1, y).checkAndAdd()
            Position(x, y + 1).checkAndAdd()
            Position(x - 1, y).checkAndAdd()
        }

        return emptyTerritoryPositions
    }

    fun Position.getState(): DotState {
        return DotState(dots[y * realWidth + x])
    }

    private fun Position.setState(state: DotState) {
        this@Field.dots[y * realWidth + x] = state.value
    }

    fun Position.isBorder(): Boolean {
        return x < OFFSET || x >= width + OFFSET || y < OFFSET || y >= height + OFFSET
    }

    override fun toString(): String = render()
}

data class MoveResult(
    val position: Position,
    val player: Player,
    val number: Int,
    val previousState: DotState,
    val extraPreviousStates: Map<Position, DotState>,
    val bases: List<Base>,
) {
    val positionPlayer: PositionPlayer
        get() = PositionPlayer(position, player)
}

class Base(
    val player: Player,
    val playerDiff: Int,
    val oppositePlayerDiff: Int,
    val closurePositions: List<Position>,
    val previousStates: Map<Position, DotState>,
    val isReal: Boolean,
)

data class PositionPlayer(
    val position: Position,
    val player: Player,
)