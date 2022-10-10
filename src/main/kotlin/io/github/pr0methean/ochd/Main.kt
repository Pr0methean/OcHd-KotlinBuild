package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.Task
import io.github.pr0methean.ochd.tasks.await
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
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

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
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
    val coroutineContext = newFixedThreadPoolContext(PARALLELISM, "Main coroutine context")
    val scope = CoroutineScope(coroutineContext).plus(supervisorJob)
    val cbScope = CoroutineScope(coroutineContext.limitedParallelism(1)).plus(supervisorJob)
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
    val time = measureNanoTime {
        stats.onTaskLaunched("Build task graph", "Build task graph")
        val tasks = ALL_MATERIALS.outputTasks(ctx).toList()
        tasks.forEach(Task<*>::registerRecursiveDependencies)
        val cbTasks = tasks.filter(OutputTask::isCommandBlock)
        val nonCbTasks = tasks.filterNot(OutputTask::isCommandBlock)
        val hugeTaskCache = ctx.hugeTileBackingCache
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        System.gc()
        val tasksRun = LongAdder()
        runAll(cbTasks, cbScope, tasksRun, stats)
        hugeTaskCache.invalidateAll()
        System.gc()
        runAll(nonCbTasks, scope, tasksRun, stats)
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after $time ns")
    exitProcess(0)
}

private suspend fun runAll(
    tasks: Iterable<OutputTask>,
    scope: CoroutineScope,
    tasksRun: LongAdder,
    stats: ImageProcessingStats
) {
    val remainingTasks = tasks.toMutableSet()
    var attemptNumber = 1
    while (remainingTasks.isNotEmpty()) {
        val pendingTasks = ConcurrentHashMap.newKeySet<Job>()
        val tasksToRetry = ConcurrentLinkedDeque<OutputTask>()
        val tasksToAttempt = remainingTasks.toMutableSet()
        while (tasksToAttempt.isNotEmpty()) {
            yield()
            val task = tasksToAttempt.minWithOrNull(
                compareByDescending(OutputTask::isCommandBlock)
                    .thenBy { (it.uncachedSubtasks() + 1.0) / (it.andAllDependencies().size + 2.0) })!!
            if (tasksToAttempt.remove(task)) {
                pendingTasks.add(scope.launch {
                    logger.info("Joining {}", task)
                    tasksRun.increment()
                    val result = runBlocking { task.await() }
                    if (result.isSuccess) {
                        logger.info("Joined {} with result of success", task)
                        task.source.removeDirectDependentTask(task)
                    } else {
                        logger.error("Error in {}", task, result.exceptionOrNull())
                        task.clearFailure()
                        tasksToRetry.add(task)
                    }
                })
            }
        }
        remainingTasks.clear()
        pendingTasks.joinAll()
        if (tasksToRetry.isNotEmpty()) {
            remainingTasks.addAll(tasksToRetry)
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

