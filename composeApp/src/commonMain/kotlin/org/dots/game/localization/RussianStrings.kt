package org.dots.game.localization

import org.dots.game.core.BaseMode
import org.dots.game.core.InitPosType
import org.dots.game.views.ConnectionDrawMode
import org.dots.game.views.PolygonDrawMode

object RussianStrings : Strings {
    // Common UI
    override val new = "Новая"
    override val reset = "Сбросить"
    override val load = "Загрузить"
    override val save = "Сохранить"
    override val settings = "Настройки"
    override val open = "Открыть"
    override val browse = "Обзор"
    override val create = "Создать"

    // Game info
    override val width = "Ширина"
    override val height = "Высота"
    override val move = "Ход"
    override val game = "Игра"
    override val komi = "Коми"

    // New Game Dialog
    override val initPosType = "Стартовая позиция"
    override val baseMode = "Режим баз"
    override val captureByBorder = "Захват через край"
    override val suicideAllowed = "Самоубийство разрешено"
    override val roundDraw = "Возможность ничьи"
    override val createNewGame = "Создать новую игру"
    override val randomStartPosition = "Случайная стартовая позиция"

    // InitPosType enum labels
    override fun initPosTypeLabel(type: InitPosType): String = when (type) {
        InitPosType.Empty -> "Нет"
        InitPosType.Single -> "Точка"
        InitPosType.Cross -> "Скрест"
        InitPosType.DoubleCross -> "Двойной скрест"
        InitPosType.QuadrupleCross -> "4X"
        InitPosType.Custom -> "Пользовательская"
    }

    // BaseMode enum labels
    override fun baseModeLabel(mode: BaseMode): String = when (mode) {
        BaseMode.AtLeastOneOpponentDot -> "Захват при одной точке"
        BaseMode.AnySurrounding -> "Захват пустой области"
        BaseMode.AllOpponentDots -> "Захват при полном заполнении"
    }

    // Open Dialog
    override val pathOrContent = "Путь или содержимое"
    override val pathOrContentPlaceholder = "Введите путь к .sgf файлу или его содержимое"
    override val rewindToEnd = "Просмотр с конца"
    override val addFinishingMove = "Добавить завершающий ход"
    override val openSgfFile = "Открыть SGF файл"

    // Save Dialog
    override val sgf = "SGF"
    override val fieldRepresentation = "Отображение поля"
    override val printNumbers = "Печатать номера"
    override val printCoordinates = "Печатать координаты"
    override val debugInfo = "Отладочная информация"
    override val padding = "Отступ"

    // Settings Dialog
    override val connectionDrawMode = "Отрисовка соединений"
    override val polygonDrawMode = "Отрисовка окружений"
    override val diagonalConnections = "Диагональные соединения"
    override val threats = "Угрозы окружения"
    override val surroundings = "Области под угрозой"
    override val developerMode = "Режим разработчика"

    // ConnectionDrawMode enum labels
    override fun connectionDrawModeLabel(mode: ConnectionDrawMode): String = when (mode) {
        ConnectionDrawMode.None -> "Нет"
        ConnectionDrawMode.Lines -> "Линии"
        ConnectionDrawMode.PolygonOutline -> "Контуры полигонов"
        ConnectionDrawMode.PolygonFill -> "Заливка полигонов"
        ConnectionDrawMode.PolygonOutlineAndFill -> "Контуры и заливка полигонов"
    }

    // PolygonDrawMode enum labels
    override fun polygonDrawModeLabel(mode: PolygonDrawMode): String = when (mode) {
        PolygonDrawMode.Outline -> "Контур"
        PolygonDrawMode.Fill -> "Заливка"
        PolygonDrawMode.OutlineAndFill -> "Контур и заливка"
    }

    // Language settings
    override val language = "Язык"
    override val languageName = "Русский"
}
