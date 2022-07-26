package io.github.pr0methean.ochd
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.logging.log4j.LogManager
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val logger = run {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    LogManager.getRootLogger()
}
var MEMORY_INTENSE_COROUTINE_CONTEXT: CoroutineDispatcher = Dispatchers.Unconfined

@Suppress("UnstableApiUsage")
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun main(args:Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    System.setProperty("glass.platform","Monocle")
    System.setProperty("monocle.platform","Headless")
    Platform.startup {}
    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")
    val scope = CoroutineScope(Dispatchers.Default)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val metadataDirectory = Paths.get("metadata").toAbsolutePath().toFile()
    val out = Paths.get("out").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")
    val ioScope = CoroutineScope(Dispatchers.IO)
    val cleanupJob = ioScope.launch(CoroutineName("Delete old outputs")) {out.deleteRecursively()}
    val ctx = ImageProcessingContext(
        name = "MainContext",
        tileSize = tileSize,
        scope = scope,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot
    )
    MEMORY_INTENSE_COROUTINE_CONTEXT = Executors.newFixedThreadPool(1.shl(24) / (tileSize * tileSize)).asCoroutineDispatcher()
    doJfx("Increase rendering thread priority") {
        Thread.currentThread().priority = Thread.MAX_PRIORITY
    }
    val stats = ctx.stats
    startMonitoring(stats, scope)
    val time = measureNanoTime {
        val copyMetadata = ioScope.plus(CoroutineName("Copy metadata files")).launch {
            cleanupJob.join()
            stats.onTaskLaunched("Copy metadata files", "Copy metadata files")
            metadataDirectory.walkTopDown().forEach {
                val outputPath = out.resolve(it.relativeTo(metadataDirectory))
                if (it.isDirectory) {
                    outputPath.mkdirs()
                } else {
                    it.copyTo(outputPath)
                }
            }
            stats.onTaskCompleted("Copy metadata files", "Copy metadata files")
        }
        stats.onTaskLaunched("Build task graph", "Build task graph")
        var tasks = ALL_MATERIALS.outputTasks(ctx)
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupJob.join()
        var retrying = false
        while (tasks.firstOrNull() != null) {
            if (retrying) {
                logger.info("Retrying these failed tasks: {}", tasks)
                System.gc()
            }
            tasks = tasks.flowOn(Dispatchers.Default.limitedParallelism(1)).map {
                scope.plus(CoroutineName(it.name)).launch { it.run() }
                it
            }.filter {
                val result = it.join()
                if (result.isFailure) {
                    it.clearFailure()
                    stats.retries.increment()
                    logger.error("Error in {}", it, result.exceptionOrNull())
                    true
                } else {
                    false
                }
            }.toList().asFlow()
            retrying = true
        }
        copyMetadata.join()
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after $time ns")
    exitProcess(0)
}