package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.await
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.plus
import kotlinx.coroutines.selects.select
import org.apache.logging.log4j.LogManager
import java.nio.file.Paths
import java.util.Comparator.comparingInt
import java.util.concurrent.atomic.LongAdder
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val taskOrderComparator = comparingInt(OutputTask::cachedSubtasks).reversed()
    .then(comparingInt(OutputTask::unstartedCacheableSubtasks))
private val logger = LogManager.getRootLogger()
private const val PARALLELISM = 3
private const val HUGE_TILE_PARALLELISM = 1

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
        val depsBuildTasks = tasks.map {task -> scope.launch {task.registerRecursiveDependencies()}}
        val cbTasks = tasks.filter(OutputTask::isCommandBlock)
        val nonCbTasks = tasks.filterNot(OutputTask::isCommandBlock)
        val hugeTaskCache = ctx.hugeTileBackingCache
        depsBuildTasks.joinAll()
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        System.gc()
        val tasksRun = LongAdder()
        runAll(cbTasks, cbScope, tasksRun, stats, HUGE_TILE_PARALLELISM)
        stats.readHugeTileCache(hugeTaskCache)
        hugeTaskCache.invalidateAll()
        System.gc()
        runAll(nonCbTasks, scope, tasksRun, stats, PARALLELISM)
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after $time ns")
    exitProcess(0)
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun runAll(
    tasks: Iterable<OutputTask>,
    scope: CoroutineScope,
    tasksRun: LongAdder,
    stats: ImageProcessingStats,
    parallelism: Int
) {
    val remainingTasks = tasks.toMutableSet()
    val pendingTasks = LinkedHashSet<ReceiveChannel<Unit>>()
    val tasksToAttempt = remainingTasks.toMutableSet()
    while (remainingTasks.isNotEmpty()) {
        while (pendingTasks.size >= parallelism || (tasksToAttempt.isEmpty() && remainingTasks.isNotEmpty())) {
            select<Unit> {
                pendingTasks.map {task -> task.onReceive {pendingTasks.remove(task)}}
            }
        }
        val task = tasksToAttempt.minWithOrNull(taskOrderComparator) ?: break
        if (tasksToAttempt.remove(task)) {
            pendingTasks.add(scope.produce {
                logger.info("Joining {}", task)
                tasksRun.increment()
                val result = task.await()
                if (result.isSuccess) {
                    logger.info("Joined {} with result of success", task)
                    task.source.removeDirectDependentTask(task)
                    remainingTasks.remove(task)
                } else {
                    logger.error("Error in {}", task, result.exceptionOrNull())
                    stats.recordRetries(1)
                    task.clearFailure()
                    tasksToAttempt.add(task)
                }
                send(Unit)
            })
        }
    }
}

