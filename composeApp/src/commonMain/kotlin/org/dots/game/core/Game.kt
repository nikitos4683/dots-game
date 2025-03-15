package org.dots.game.core

class Game(val gameInfo: GameInfo, val gameTree: GameTree) {
    val rules = gameTree.field.rules

    operator fun component1(): GameInfo = gameInfo
    operator fun component2(): Rules = rules
    operator fun component3(): GameTree = gameTree
}

class GameInfo(
    val appInfo: AppInfo?,
    val gameName: String?,
    val player1Name: String?,
    val player1Rating: Double?,
    val player1Team: String?,
    val player2Name: String?,
    val player2Rating: Double?,
    val player2Team: String?,
    val komi: Double?,
    val date: String?,
    val description: String?,
    val comment: String?,
    val place: String?,
    val event: String?,
    val opening: String?,
    val annotator: String?,
    val copyright: String?,
    val source: String?,
    val time: Double?,
    val overtime: String?,
    val result: GameResult?,
    val round: String?,
)

data class AppInfo(val name: String, val version: String?) {
    val appType = AppType.entries.find { it.value == name } ?: AppType.Unknown
}

sealed class GameResult {
    object Draw : GameResult()

    sealed class WinGameResult(val player: Player) : GameResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            return player == (other as WinGameResult).player
        }

        override fun hashCode(): Int {
            return player.hashCode()
        }
    }

    class ScoreWin(val score: Double, player: Player) : WinGameResult(player) {
        override fun equals(other: Any?): Boolean {
            if (!super.equals(other)) return false
            return score == (other as ScoreWin).score
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + score.hashCode()
            return result
        }
    }

    class ResignWin(player: Player) : WinGameResult(player)

    class TimeWin(player: Player) : WinGameResult(player)

    class UnknownWin(player: Player) : WinGameResult(player)
}

data class MoveInfo(val position: Position, val player: Player, val extraInfo: Any? = null)

data class Label(val position: Position, val text: String)

enum class AppType(val value: String) {
    Zagram("zagram.org"),
    Notago("NOTAGO"),
    Playdots("Спортивные Точки (playdots.ru)"),
    Unknown(""),
}