package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.AbstractTask
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.await
import io.github.pr0methean.ochd.tasks.caching.SemiStrongTaskCache
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
private val taskOrderComparator = comparingLong(OutputTask::timesFailed)
    .then(comparingInt(OutputTask::startedOrAvailableSubtasks).reversed())
    .then(comparingInt(OutputTask::cacheableSubtasks))
private val logger = LogManager.getRootLogger()
private val PARALLELISM = Runtime.getRuntime().availableProcessors()
private const val HUGE_TILE_PARALLELISM = 1
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
    val coroutineContext = newFixedThreadPoolContext(PARALLELISM, "Main coroutine context")
    val scope = CoroutineScope(coroutineContext).plus(supervisorJob)
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
        val tasks = ALL_MATERIALS.outputTasks(ctx).map { ctx.deduplicate(it) as OutputTask }.toSet()
        val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() }}
        val cbTasks = tasks.filter(OutputTask::isCommandBlock)
        val nonCbTasks = tasks.filterNot(OutputTask::isCommandBlock)
        val hugeTaskCache = ctx.hugeTileBackingCache
        depsBuildTask.join()
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        System.gc()
        runAll(cbTasks, scope, stats, HUGE_TILE_PARALLELISM)
        stats.readHugeTileCache(hugeTaskCache)
        hugeTaskCache.invalidateAll()
        System.gc()
        runAll(nonCbTasks, scope, stats, PARALLELISM)
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after {} ns", Unbox.box(time))
    exitProcess(0)
}

data class TaskResult(val task: OutputTask, val succeeded: Boolean)

private suspend fun runAll(
    tasks: Iterable<OutputTask>,
    scope: CoroutineScope,
    stats: ImageProcessingStats,
    parallelism: Int
) {
    val unstartedTasks = tasks.sortedWith(comparingInt(OutputTask::cacheableSubtasks)).toMutableSet()
    val unfinishedTasks = AtomicLong(unstartedTasks.size.toLong())
    val inProgressJobs = mutableMapOf<OutputTask,Job>()
    val finishedJobsChannel = Channel<TaskResult>(capacity = CAPACITY_PADDING_FACTOR * parallelism)
    var maxRetriesAnyTaskSoFar = 0L
    while (unfinishedTasks.get() > 0) {
        check(inProgressJobs.isNotEmpty() || unstartedTasks.isNotEmpty()) {
            "Have ${unfinishedTasks.get()} unfinished tasks, but none are in progress"
        }
        val maybeReceive = finishedJobsChannel.tryReceive().getOrElse {
            if (inProgressJobs.size >= parallelism
                    || (inProgressJobs.isNotEmpty() && unstartedTasks.isEmpty())) {
                logger.debug("{} tasks remain. Waiting for one of: {}", Unbox.box(unfinishedTasks.get()), inProgressJobs)
                inProgressJobs.forEach {(task, job) ->
                    if (job.start()) {
                        logger.warn("Had to start the job for {} in the fallback loop!", task)
                    }
                }
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
        val timesFailed = task.timesFailed()
        if (timesFailed > maxRetriesAnyTaskSoFar) {
            check(timesFailed <= GLOBAL_MAX_RETRIES) { "Too many failures in $task!" }
            maxRetriesAnyTaskSoFar = timesFailed
            val cache = (task.source as? AbstractTask<*>)?.cache
            if (cache is SemiStrongTaskCache<*>) {
                cache.clearPrimaryCache()
            }
        }
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
