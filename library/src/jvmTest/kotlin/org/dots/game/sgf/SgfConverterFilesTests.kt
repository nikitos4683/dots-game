package org.dots.game.sgf

import org.dots.game.buildLineOffsets
import org.dots.game.toLineColumnDiagnostic
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.test.Test

class SgfConverterFilesTests {
    private val nanosInSec: Long = TimeUnit.SECONDS.toNanos(1)

    private fun processFileOrDirectory(
        fileOrDirectoryPath: String,
        logFile: File?,
        numberOfFilesToDrop: Int = 0,
        numberOfFilesToProcess: Int = Int.MAX_VALUE
    ) {
        val fileOrDirectory = File(fileOrDirectoryPath)

        val isDirectory: Boolean
        val sgfFiles = if (fileOrDirectory.isDirectory) {
            isDirectory = true
            fileOrDirectory.walkTopDown()
                .filter { it.isFile && it.extension == "sgf" }
                .drop(numberOfFilesToDrop)
                .take(numberOfFilesToProcess)
                .toList()
                .takeIf { it.isNotEmpty() } ?: run {
                    println("The directory ${fileOrDirectory.absolutePath} does not contain sgf files")
                    return
                }
        } else {
            isDirectory = false
            fileOrDirectory.takeIf { it.extension == "sgf" }?.let { listOf(it) } ?: run {
                println("The file ${fileOrDirectory.absolutePath} does not have sgf extension")
                return
            }
        }

        val fileOutputWriter = if (logFile != null) {
            FileOutputStream(logFile, false).bufferedWriter()
        } else {
            null
        }

        val diagnosticsLogger = { message: String ->
            if (fileOutputWriter != null) {
                fileOutputWriter.write (message + "\n")
            } else {
                println(message)
            }
        }
        val exceptionLogger = { file: File, exception: Exception ->
            "EXCEPTION on $file: $exception".let { message ->
                fileOutputWriter?.write(message + "\n")
                println(message)
            }
        }

        val filesNumber = sgfFiles.size
        println("Number of sgf files: $filesNumber")
        println()

        var totalParserNanos = 0L
        var totalConverterNanos = 0L
        var totalFieldNanos = 0L
        var totalMovesCount = 0
        var progress = 0

        val totalTimeStart = System.nanoTime()

        for ((index, file) in sgfFiles.withIndex()) {
            val processingResult = processFile(file, diagnosticsLogger, exceptionLogger)
            if (processingResult != null) {
                totalParserNanos += processingResult.parserNanos
                totalConverterNanos += processingResult.converterNanos
                totalFieldNanos += processingResult.fieldNanos
                totalMovesCount += processingResult.movesCount
            }

            val currentProgress = round((index.toDouble() / filesNumber) * 100).toInt()
            if (currentProgress > progress && isDirectory) {
                fileOutputWriter?.flush()
                progress = currentProgress
                println("Progress: $progress")
            }
        }

        fileOutputWriter?.close()

        val totalSgfNanos = (totalParserNanos + totalConverterNanos + totalFieldNanos).toDouble()
        val totalTime = System.nanoTime() - totalTimeStart

        if (isDirectory) {
            fun printTime(name: String, value: Long) {
                println("$name time: ${TimeUnit.NANOSECONDS.toMillis(value)} ms (${"%.2f".format(value / totalSgfNanos * 100)} %)")
            }

            println()
            printTime("Parser", totalParserNanos)
            printTime("Converter", totalConverterNanos)
            printTime("Field", totalFieldNanos)
            println("Total time: ${TimeUnit.NANOSECONDS.toMillis(totalTime)} ms")
            println("Total files count: ${sgfFiles.size}")
            println("Total moves count: $totalMovesCount")
            println()

            println("Field moves per second: ${(totalMovesCount.toDouble() / totalFieldNanos * nanosInSec).toInt()}")
            println("Fields per second: ${(sgfFiles.size.toDouble() / totalFieldNanos * nanosInSec).toInt()}")
            println("Millis per field: ${totalFieldNanos.toDouble() / sgfFiles.size / TimeUnit.MILLISECONDS.toNanos(1)}")
            println("Average number of moves per field: ${(totalMovesCount.toDouble() / sgfFiles.size).toInt()}")
        }
    }

    private fun processFile(file: File, diagnosticsLogger: (String) -> Unit, exceptionLogger: (File, Exception) -> Unit): ProcessingResult? {
        try {
            val content = file.readText()
            val lineOffsets by lazy { content.buildLineOffsets() }
            var errorCount = 0

            val parserStartNanos = System.nanoTime()
            val sgfParseTree = SgfParser.parse(content) { parseDiagnostic ->
                diagnosticsLogger(parseDiagnostic.toLineColumnDiagnostic(lineOffsets).toString())
                errorCount++
            }
            val parserElapsedNanos = System.nanoTime() - parserStartNanos

            val converterAndFieldStartNanos = System.nanoTime()
            val sgfConverter = SgfConverter(sgfParseTree, warnOnMultipleGames = false, measureNanos = {
                System.nanoTime()
            }) { convertDiagnostic ->
                diagnosticsLogger(convertDiagnostic.toLineColumnDiagnostic(lineOffsets).toString())
                errorCount++
            }
            val games = sgfConverter.convert()
            val movesCount = games.sumOf { it.gameTree.allNodesCount }
            val converterAndFieldElapsedNanos = System.nanoTime() - converterAndFieldStartNanos

            val fieldNanos = sgfConverter.fieldNanos
            val converterNanos = converterAndFieldElapsedNanos - fieldNanos

            if (errorCount > 0 && games.isNotEmpty()) {
                diagnosticsLogger("$file contains errors and has the following rules: ${games.first().gameInfo.comment}")
            }

            return ProcessingResult(parserElapsedNanos, converterNanos, fieldNanos, movesCount)
        } catch (e: Exception) {
            exceptionLogger(file, e)
        }
        return null
    }

    private data class ProcessingResult(
        val parserNanos: Long,
        val converterNanos: Long,
        val fieldNanos: Long,
        val movesCount: Int,
    )
}
