package io.github.pr0methean.ochd

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.FileOutputTask
import io.github.pr0methean.ochd.tasks.await
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
import kotlinx.coroutines.flow.filter
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
private val taskOrderComparator = comparingLong(FileOutputTask::timesFailed)
    .then(comparingInt(FileOutputTask::startedOrAvailableSubtasks).reversed())
    .then(comparingInt(FileOutputTask::cacheableSubtasks))
private val logger = LogManager.getRootLogger()
private val PARALLELISM = Runtime.getRuntime().availableProcessors()
private const val HUGE_TILE_PARALLELISM = 1
private const val GLOBAL_MAX_RETRIES = 100L
private const val MAIN_CACHE_SIZE_BYTES = 1L.shl(31)
private const val HUGE_TILE_CACHE_SIZE_BYTES = 1L.shl(29)
private const val REGULAR_TILES_PER_HUGE_TILE = 4

private const val BYTES_PER_PIXEL = 4

@OptIn(DelicateCoroutinesApi::class)
@Suppress("UnstableApiUsage", "DeferredResultUnused")
suspend fun main(args: Array<String>) {
    ImageIO.setUseCache(false) // Prevent intermediate disk writes when real destination is a ByteArrayOutputStream
    if (args.size < 2) {
        println("Usage: main <size> <hugeTileJobs>")
        return
    }
    val tileSize = args[0].toInt()
    require(tileSize > 0) { "tileSize shouldn't be zero or negative but was ${args[0]}" }
    val hugeTileJobs = args[1].toBoolean()
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
    val coroutineContext = newFixedThreadPoolContext(PARALLELISM, "Main coroutine context")
    val scope = CoroutineScope(coroutineContext).plus(supervisorJob)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")
    val bytesPerTile = BYTES_PER_PIXEL * tileSize * tileSize
    val ctx = TaskPlanningContext(
        name = "MainContext",
        tileSize = tileSize,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot,
        backingCache = if (hugeTileJobs) {
            Caffeine.newBuilder()
                .recordStats()
                .weakKeys()
                .executor(Runnable::run) // keep eviction on same thread as population
                .maximumSize(HUGE_TILE_CACHE_SIZE_BYTES / (REGULAR_TILES_PER_HUGE_TILE * bytesPerTile))
                .build()
        } else {
            Caffeine.newBuilder()
                .recordStats()
                .weakKeys()
                .executor(Runnable::run) // keep eviction on same thread as population
                .maximumSize(MAIN_CACHE_SIZE_BYTES / bytesPerTile)
                .build()
        }
    )
    doJfx("Increase rendering thread priority") {
        Thread.currentThread().priority = Thread.MAX_PRIORITY
    }
    val stats = ctx.stats
    startMonitoring(stats, scope)
    val time = measureNanoTime {
        stats.onTaskLaunched("Build task graph", "Build task graph")
        val tasks = ALL_MATERIALS.outputTasks(ctx)
            .map { ctx.deduplicate(it) as FileOutputTask }
            .filter { it.isCommandBlock == hugeTileJobs }.toSet()
        val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() }}
        depsBuildTask.join()
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        System.gc()
        runAll(tasks, scope, stats, HUGE_TILE_PARALLELISM)
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after {} ns", Unbox.box(time))
    exitProcess(0)
}

data class TaskResult(val task: FileOutputTask, val succeeded: Boolean)

private suspend fun runAll(
    tasks: Iterable<FileOutputTask>,
    scope: CoroutineScope,
    stats: ImageProcessingStats,
    parallelism: Int
) {
    val unstartedTasks = tasks.sortedWith(comparingInt(FileOutputTask::cacheableSubtasks)).toMutableSet()
    val unfinishedTasks = AtomicLong(unstartedTasks.size.toLong())
    val inProgressJobs = mutableMapOf<FileOutputTask,Job>()
    val finishedJobsChannel = Channel<TaskResult>(capacity = CAPACITY_PADDING_FACTOR * parallelism)
    while (unfinishedTasks.get() > 0) {
        check(inProgressJobs.isNotEmpty() || unstartedTasks.isNotEmpty()) {
            "Have ${unfinishedTasks.get()} unfinished tasks, but none are in progress"
        }
        val maybeReceive = finishedJobsChannel.tryReceive().getOrElse {
            if (inProgressJobs.size >= parallelism
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
        check(task.timesFailed.get() <= GLOBAL_MAX_RETRIES) { "Too many failures in $task!" }
        inProgressJobs[task] = scope.launch {
            logger.info("Joining {}", task)
            val result = task.await()
            if (result.isSuccess) {
                unfinishedTasks.getAndDecrement()
                finishedJobsChannel.send(TaskResult(task, true))
            } else {
                finishedJobsChannel.send(TaskResult(task, false))
                logger.error("Joined {} with an error: {}", task, result.exceptionOrNull()?.message)
                stats.recordRetries(1)
            }
        }
    }
    logger.debug("All jobs done; closing channel")
    finishedJobsChannel.close()
}
