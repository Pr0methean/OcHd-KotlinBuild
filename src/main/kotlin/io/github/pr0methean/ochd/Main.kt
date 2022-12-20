package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.tasks.doJfx
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.plus
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.nio.file.Paths
import java.util.Comparator.comparingInt
import java.util.Comparator.comparingLong
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private const val CAPACITY_PADDING_FACTOR = 2
private val taskOrderComparator = comparingLong(PngOutputTask::timesFailed)
    .then(comparingInt(PngOutputTask::startedOrAvailableSubtasks).reversed())
    .then(comparingInt(PngOutputTask::cacheableSubtasks))
private val logger = LogManager.getRootLogger()
private const val THREADS_PER_CPU = 1.0
private val THREADS = perCpu(THREADS_PER_CPU)
private const val MAX_OUTPUT_TASKS_PER_CPU = 1.5
private val MAX_OUTPUT_TASKS = perCpu(MAX_OUTPUT_TASKS_PER_CPU)
private const val MAX_HUGE_TILE_OUTPUT_TASKS_PER_CPU = 1.0
private val MAX_HUGE_TILE_OUTPUT_TASKS = perCpu(MAX_HUGE_TILE_OUTPUT_TASKS_PER_CPU)
private const val MIN_TILE_SIZE_FOR_EXPLICIT_GC = 512

private fun perCpu(amount: Double) = (amount * Runtime.getRuntime().availableProcessors()).toInt()
private const val GLOBAL_MAX_RETRIES = 100L

@OptIn(DelicateCoroutinesApi::class)
@Suppress("UnstableApiUsage", "DeferredResultUnused")
suspend fun main(args: Array<String>) {
    ImageIO.setUseCache(false) // Prevent intermediate disk writes when real destination is a ByteArrayOutputStream
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    val tileSize = args[0].toInt()
    require(tileSize > 0) { "tileSize shouldn't be zero or negative but was ${args[0]}" }
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
    val coroutineContext = newFixedThreadPoolContext(THREADS, "Main coroutine context")
    val scope = CoroutineScope(coroutineContext).plus(supervisorJob)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")

    val ctx = TaskPlanningContext(
        name = "MainContext",
        tileSize = tileSize,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot,
        ctx = coroutineContext
    )
    doJfx("Increase rendering thread priority") {
        Thread.currentThread().priority = Thread.MAX_PRIORITY
    }
    val stats = ctx.stats
    startMonitoring(stats, scope)
    val time = measureNanoTime {
        stats.onTaskLaunched("Build task graph", "Build task graph")
        val tasks = ALL_MATERIALS.outputTasks(ctx).map { ctx.deduplicate(it) as PngOutputTask }.toSet()
        val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() }}
        val cbTasks = tasks.filter(PngOutputTask::isCommandBlock)
        val nonCbTasks = tasks.filterNot(PngOutputTask::isCommandBlock)
        val hugeTaskCache = ctx.hugeTileBackingCache
        depsBuildTask.join()
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        gcIfUsingLargeTiles(tileSize)
        runAll(cbTasks, scope, stats, MAX_HUGE_TILE_OUTPUT_TASKS)
        stats.readHugeTileCache(hugeTaskCache)
        hugeTaskCache.invalidateAll()
        gcIfUsingLargeTiles(tileSize)
        runAll(nonCbTasks, scope, stats, MAX_OUTPUT_TASKS)
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after {} ns", Unbox.box(time))
    exitProcess(0)
}

fun gcIfUsingLargeTiles(tileSize: Int) {
    if (tileSize >= MIN_TILE_SIZE_FOR_EXPLICIT_GC) {
        System.gc()
    }
}

data class TaskResult(val task: PngOutputTask, val succeeded: Boolean)

private suspend fun runAll(
    tasks: Iterable<PngOutputTask>,
    scope: CoroutineScope,
    stats: ImageProcessingStats,
    maxJobs: Int
) {
    val unstartedTasks = tasks.sortedWith(comparingInt(PngOutputTask::cacheableSubtasks)).toMutableSet()
    val unfinishedTasks = AtomicLong(unstartedTasks.size.toLong())
    val inProgressJobs = mutableMapOf<PngOutputTask,Job>()
    val finishedJobsChannel = Channel<TaskResult>(capacity = CAPACITY_PADDING_FACTOR * THREADS)
    while (unfinishedTasks.get() > 0) {
        check(inProgressJobs.isNotEmpty() || unstartedTasks.isNotEmpty()) {
            "Have ${unfinishedTasks.get()} unfinished tasks, but none are in progress"
        }
        val maybeReceive = finishedJobsChannel.tryReceive().getOrElse {
            if (inProgressJobs.size >= maxJobs
                    || (inProgressJobs.isNotEmpty() && unstartedTasks.isEmpty())) {
                logger.debug("{} tasks remain. Waiting for one of: {}",
                        Unbox.box(unfinishedTasks.get()), inProgressJobs)
                finishedJobsChannel.receive()
            } else null
        }
        if (maybeReceive != null) {
            if (!maybeReceive.succeeded) {
                unstartedTasks.add(maybeReceive.task)
            }
            inProgressJobs.remove(maybeReceive.task)
            continue
        }
        val task = unstartedTasks.minWithOrNull(taskOrderComparator)
        checkNotNull(task) { "Could not get an unstarted task" }
        check(unstartedTasks.remove(task)) { "Attempted to remove task more than once: $task" }
        if(task.timesFailed.get() > GLOBAL_MAX_RETRIES) {
            logger.fatal("Too many failures in $task!")
            exitProcess(1)
        }
        inProgressJobs[task] = scope.launch {
            logger.info("Joining {}", task)
            try {
                task.await()
                task.base.removeDirectDependentTask(task)
                unfinishedTasks.getAndDecrement()
                finishedJobsChannel.send(TaskResult(task, true))
            } catch (t: Throwable) {
                task.clearCache()
                finishedJobsChannel.send(TaskResult(task, false))
                logger.error("Joined {} with {}: {}", task, t::class.simpleName, t.message)
                stats.recordRetries(1)
            }
        }
    }
    logger.debug("All jobs done; closing channel")
    finishedJobsChannel.close()
}
