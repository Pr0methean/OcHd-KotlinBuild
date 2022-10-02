package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.Task
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.LongAdder
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val logger = LogManager.getRootLogger()
private const val PARALLELISM = 2

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UnstableApiUsage", "DeferredResultUnused")
suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    val supervisorJob = SupervisorJob()
    val ioScope = CoroutineScope(Dispatchers.IO).plus(supervisorJob)
    val out = Paths.get("pngout").toAbsolutePath().toFile()
    val metadataDirectory = Paths.get("metadata").toAbsolutePath().toFile()
    val cleanupAndCopyMetadata = ioScope.launch(CoroutineName("Delete old outputs & copy metadata files")) {
        out.deleteRecursively()
        metadataDirectory.walkTopDown().forEach {
            val outputPath = out.resolve(it.relativeTo(metadataDirectory))
            if (it.isDirectory) {
                outputPath.mkdirs()
            } else {
                it.copyTo(outputPath)
            }
        }
    }
    Platform.startup {}
    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")
    val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(PARALLELISM)).plus(supervisorJob)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")

    val ctx = TaskPlanningContext(
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
    var attemptNumber = 1
    val time = measureNanoTime {
        stats.onTaskLaunched("Build task graph", "Build task graph")
        var tasks = ALL_MATERIALS.outputTasks(ctx).toList()
        tasks.forEach(Task<*>::registerRecursiveDependencies)
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        System.gc()
        val tasksRun = LongAdder()
        val pendingTasks = ConcurrentHashMap.newKeySet<Job>()
        while (tasks.isNotEmpty()) {
            val tasksToRetry = ConcurrentLinkedDeque<OutputTask>()
            val taskSet = tasks.toMutableSet()
            while (taskSet.isNotEmpty()) {
                yield()
                val task = taskSet.minBy {
                    (if (it.name.contains("command_block")) -1.0e6 else 0.0) +
                    (it.uncachedSubtasks() + 1.0) / (it.andAllDependencies().size + 2.0)
                }
                taskSet.remove(task)
                pendingTasks.add(scope.launch {
                    logger.info("Joining {}", task)
                    tasksRun.increment()
                    val result = task.await()
                    if (result.isSuccess) {
                        logger.info("Joined {} with result of success", task)
                        task.source.removeDirectDependentTask(task)
                    } else {
                        logger.error("Error in {}", task, result.exceptionOrNull())
                        task.clearFailure()
                        logger.debug("Cleared failure in {}", task)
                        tasksToRetry.add(task)
                    }
                })
            }
            pendingTasks.joinAll()
            tasks = tasksToRetry.toList()
            if (tasksToRetry.isNotEmpty()) {
                System.gc()
                logger.warn(
                    "{} tasks succeeded and {} failed on attempt {}",
                    box(tasksRun.sumThenReset() - tasksToRetry.size), box(tasksToRetry.size), box(attemptNumber)
                )
                stats.recordRetries(tasksToRetry.size.toLong())
                attemptNumber++
            }
        }
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after $time ns")
    exitProcess(0)
}
