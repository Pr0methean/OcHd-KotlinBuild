package io.github.pr0methean.ochd
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import java.nio.file.Paths
import java.util.logging.Logger
import kotlin.system.measureNanoTime

private val logger = run {
    val ctx: LoggerContext = LogManager.getContext(false) as LoggerContext
    val config: Configuration = ctx.configuration
    val loggerConfig: LoggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
    loggerConfig.level = Level.TRACE
    ctx.updateLoggers()
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    Logger.getGlobal()
}
suspend fun main(args:Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    System.setProperty("glass.platform","Monocle")
    System.setProperty("monocle.platform","Headless")

    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")
    val scope = CoroutineScope(Dispatchers.Default)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val metadataDirectory = Paths.get("metadata").toAbsolutePath().toFile()
    val out = Paths.get("out").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")
    out.deleteRecursively()
    outTextureRoot.mkdirs()
    val pngDirectory = Paths.get("png").toAbsolutePath().toFile()
    pngDirectory.deleteRecursively()
    pngDirectory.mkdirs()
    val ctx = ImageProcessingContext(
        name = "MainContext",
        tileSize = tileSize,
        scope = scope,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot
    )
    // List constants
    /*
val SIMPLE_ORES = listOf("coal", "copper", "iron", "redstone", "gold", "quartz")
val ORES = listOf(SIMPLE_ORES, listOf("lapis", "diamond", "emerald")).flatten()
val COMMAND_BLOCK_TYPES = listOf("command_block", "repeating_command_block", "chain_command_block")
val NORMAL_MUSIC_DISCS = listOf("far", "wait", "strad", "mall", "cat", "pigstep", "mellohi", "13", "blocks", "stal",
        "ward", "5", "otherside", "chirp")
val DISC_LABEL_COLORS = listOf(DYES.values).subList(1, DYES.values.size - 1)
val OXIDATION_STATES = listOf("exposed", "weathered", "oxidized")
 */
    ctx.startMonitoringStats()
    val time = measureNanoTime {
        val tasks = mutableListOf(
        scope.async { withContext(Dispatchers.IO) {
            metadataDirectory.walkTopDown().forEach {
                val outputPath = out.resolve(it.relativeTo(metadataDirectory))
                if (it.isDirectory) {
                    outputPath.mkdirs()
                } else {
                    it.copyTo(outputPath)
                }
            }
        }})
        ALL_MATERIALS.outputTasks(ctx).map { scope.async { it.run() } }.toList(tasks)
        tasks.awaitAll()
    }
    logger.info("")
    logger.info("All tasks finished after $time ns")
    ctx.printStats()
}
