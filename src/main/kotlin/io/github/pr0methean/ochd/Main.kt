package io.github.pr0methean.ochd
import io.github.pr0methean.ochd.materials.item.ITEMS
import io.github.pr0methean.ochd.tasks.consumable.doJfx
import javafx.application.Platform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.nio.file.Paths
import kotlin.Result.Companion.failure
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val logger = run {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    LogManager.getRootLogger()
}

@Suppress("UnstableApiUsage", "DeferredResultUnused")
suspend fun main(args:Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    System.setProperty("glass.platform","Monocle")
    System.setProperty("monocle.platform","Headless")
    Platform.startup {}
    val supervisorJob = SupervisorJob()
    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")
    val scope = CoroutineScope(Dispatchers.Default).plus(supervisorJob)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val metadataDirectory = Paths.get("metadata").toAbsolutePath().toFile()
    val out = Paths.get("pngout").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")
    val ioScope = CoroutineScope(Dispatchers.IO).plus(supervisorJob)
    val cleanupJob = ioScope.launch(CoroutineName("Delete old outputs")) {out.deleteRecursively()}
    val ctx = ImageProcessingContext(
        name = "MainContext",
        tileSize = tileSize,
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
        var tasks = ITEMS.outputTasks(ctx) // = ALL_MATERIALS.outputTasks(ctx)
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupJob.join()
        while (tasks.firstOrNull() != null) {
            tasks.collect {
                it.startAsync()
            }
            val tasksToRetry = tasks.filter {
                withContext(scope.coroutineContext.plus(CoroutineName("Joining ${it.name}"))) {
                    logger.info("Joining {}", it)
                    val result = try {
                        it.await()
                    } catch (t: Throwable) {
                        failure(t)
                    }
                    logger.info("Joined {} with result of {}", it, result)
                    if (result.isFailure) {
                        logger.error("Error in {}", it, result.exceptionOrNull())
                        true
                    } else {
                        false
                    }
                }
            }.toList()
            if (tasksToRetry.isNotEmpty()) {
                logger.info("Clearing {} failures in order to retry", Unbox.box(tasksToRetry.size))
                tasksToRetry.forEach {it.clearFailure()}
                logger.info("Retrying {} failed tasks", Unbox.box(tasksToRetry.size))
                System.gc()
            }
            tasks = tasksToRetry.asFlow()
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