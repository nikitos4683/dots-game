import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.restrictTo
import org.dots.game.core.Field
import org.dots.game.core.InitialPositionType
import org.dots.game.core.Rules
import java.io.File
import kotlin.reflect.KProperty

class CliArgs : CliktCommand() {
    private val captureEmptyBasesOption = "--empty-bases"

    val path: File? by option()
        .file(mustExist = true, mustBeReadable = true)
        .help("If specified, the tool handles the provided directory with SGF or a single SGF")
    val logFile: File? by option()
        .file(mustExist = true, mustBeWritable = true)
        .help("If specified, the tool dumps log to the file")
    val gamesCount: Int by option("-c", "--count")
        .int()
        .restrictTo(1)
        .default(1000)
        .help("Number of games to process")
    val gamesCountToDrop: Int? by option()
        .int()
        .restrictTo(0)
        .help("Number of games to drop")
    val width: Int? by option("-w", "--width")
        .int()
        .restrictTo(1, Field.MAX_WIDTH)
        .help("Field width")
    val height: Int? by option("-h", "--height")
        .int()
        .restrictTo(1, Field.MAX_HEIGHT)
        .help("Field height")
    val captureEmptyBases: Boolean? by option(captureEmptyBasesOption)
        .boolean()
        .help("If enabled, base is created even if it doesn't have enemy dots inside")
    val initialPosition: InitialPositionType? by option()
        .enum<InitialPositionType>()
        .help("The initial position, allowed values: ${InitialPositionType.Empty}, ${InitialPositionType.Cross}")
        .check { it == InitialPositionType.Empty || it == InitialPositionType.Cross }
    val seed: Long? by option("-s", "--seed")
        .long()
        .help("Seed. Use `0` value for timestamp-based seed")
    val checkRollback: Boolean by option("--check")
        .boolean()
        .default(false)
        .help("Enabled extra checks (for instance, on rollback)")

    override fun run() {
        val path = path
        if (path != null) {
            println("SGF Directory or File mode activated...")
            reportSpecifiedButUnusedParameter(::width, width)
            reportSpecifiedButUnusedParameter(::height, height)
            reportSpecifiedButUnusedParameter(captureEmptyBasesOption, captureEmptyBases)
            reportSpecifiedButUnusedParameter(::initialPosition, initialPosition)
            reportSpecifiedButUnusedParameter(::seed, seed)
            SgfAnalyser.process(path, logFile, numberOfFilesToProcess = gamesCount)
        } else {
            println("Random games mode activated...")
            reportSpecifiedButUnusedParameter(::gamesCountToDrop, gamesCountToDrop)
            RandomGameAnalyser.process(
                gamesCount,
                width ?: Rules.Standard.width,
                height ?: Rules.Standard.height,
                initialPosition ?: InitialPositionType.Empty,
                captureEmptyBases ?: false,
                seed ?: 0L,
                checkRollback,
            )
        }
    }
    fun reportSpecifiedButUnusedParameter(property: KProperty<*>, value: Any?) {
        return reportSpecifiedButUnusedParameter("--" + property.name, value)
    }

    fun reportSpecifiedButUnusedParameter(optionName: String, value: Any?) {
        if (value != null) {
            println("⚠ The parameter `${optionName}` is specified but unused")
        }
    }
}