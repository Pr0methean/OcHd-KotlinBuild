package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.LongAdder
import kotlin.Result.Companion.failure
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val logger = run {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    LogManager.getRootLogger()
}

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
    val scope = CoroutineScope(Dispatchers.Default).plus(supervisorJob)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")

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
    var attemptNumber = 1
    val time = measureNanoTime {
        stats.onTaskLaunched("Build task graph", "Build task graph")
        var tasks = ALL_MATERIALS.outputTasks(ctx).toList()
        val distances = WeakHashMap<OutputTask,WeakHashMap<OutputTask, Double>>()
        for (task in tasks) {
            distances[task] = WeakHashMap()
            for (otherTask in tasks) {
                distances[task]!![otherTask] = distanceBetween(task, otherTask)
            }
        }
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        System.gc()
        val tasksRun = LongAdder()
        var prevTask: OutputTask? = null
        while (tasks.isNotEmpty()) {
            val tasksToRetry = ConcurrentLinkedDeque<OutputTask>()
            val taskSet = tasks.toMutableSet()
            while (taskSet.isNotEmpty()) {
                val task = if (prevTask == null) {
                    taskSet.first()
                } else {
                    taskSet.minBy {
                        1L.shl(30).toDouble() * it.unstartedSubtasks()
                        + 1L.shl(20).toDouble() * it.uncachedSubtasks()
                        + 1L.shl(10).toDouble() * ((it.uncachedSubtasks().toDouble() - 2) / (it.andAllDependencies().size - 2))
                        + distances[prevTask]!![it]!!}
                }
                taskSet.remove(task)
                prevTask = task
                val result = withContext(scope.coroutineContext) {
                    logger.info("Joining {}", task)
                    try {
                        val result = task.await()
                        if (result.isSuccess) {
                            task.source.removeDependentOutputTask(task)
                        }
                        return@withContext result
                    } catch (t: Throwable) {
                        return@withContext failure<Unit>(t)
                    }
                }
                tasksRun.increment()
                if (result.isFailure) {
                    logger.error("Error in {}", task, result.exceptionOrNull())
                    task.clearFailure()
                    logger.debug("Cleared failure in {}", task)
                    tasksToRetry.add(task)
                } else {
                    logger.info("Joined {} with result of {}", task, result)
                }
            }
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

fun distanceBetween(task: OutputTask, otherTask: OutputTask): Double {
    val task1deps = task.andAllDependencies()
    val task2deps = otherTask.andAllDependencies()
    return 1.0 - (task1deps.intersect(task2deps).size.toDouble() / task1deps.union(task2deps).size)
}
