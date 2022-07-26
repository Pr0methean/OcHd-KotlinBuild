package io.github.pr0methean.ochd
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.system.measureNanoTime

private val logger = run {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    LogManager.getRootLogger()
}
val MEMORY_INTENSE_COROUTINE_CONTEXT = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

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
        val tasks = ALL_MATERIALS.outputTasks(ctx).toList()
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupJob.join()
        tasks.asFlow().flowOn(Dispatchers.Default.limitedParallelism(1)).map {
            withContext(MEMORY_INTENSE_COROUTINE_CONTEXT) {
                scope.plus(CoroutineName(it.name)).launch { it.run() }
            }
        }.collect(Job::join)
        copyMetadata.join()
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after $time ns")
}