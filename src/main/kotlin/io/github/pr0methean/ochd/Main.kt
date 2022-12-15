package io.github.pr0methean.ochd

import com.sun.management.GarbageCollectorMXBean
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.FileOutputTask
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
import kotlinx.coroutines.yield
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
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
private const val JOBS_PER_CPU = 1.0
private val PARALLELISM = (JOBS_PER_CPU * Runtime.getRuntime().availableProcessors()).toInt()
private const val GLOBAL_MAX_RETRIES = 100L
private const val MIN_FREE_MEMORY = 1024 * 1024 * 1024L
private val GC_MX_BEAN = ManagementFactory.getGarbageCollectorMXBeans().first { it is GarbageCollectorMXBean }
        as GarbageCollectorMXBean
private val MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean()

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
        val tasks = ALL_MATERIALS.outputTasks(ctx).map { ctx.deduplicate(it) as FileOutputTask }.toSet()
        val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() }}
        val cbTasks = tasks.filter(FileOutputTask::isCommandBlock)
        val nonCbTasks = tasks.filterNot(FileOutputTask::isCommandBlock)
        val hugeTaskCache = ctx.hugeTileBackingCache
        depsBuildTask.join()
        stats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        System.gc()
        runAll(cbTasks, scope, stats)
        stats.readHugeTileCache(hugeTaskCache)
        hugeTaskCache.invalidateAll()
        System.gc()
        runAll(nonCbTasks, scope, stats)
    }
    stopMonitoring()
    Platform.exit()
    stats.log()
    logger.info("")
    logger.info("All tasks finished after {} ns", Unbox.box(time))
    exitProcess(0)
}

data class TaskResult(val task: FileOutputTask, val succeeded: Boolean)

@Suppress("ComplexCondition")
private suspend fun runAll(
    tasks: Iterable<FileOutputTask>,
    scope: CoroutineScope,
    stats: ImageProcessingStats
) {
    val unstartedTasks = tasks.sortedWith(comparingInt(FileOutputTask::cacheableSubtasks)).toMutableSet()
    val unfinishedTasks = AtomicLong(unstartedTasks.size.toLong())
    val inProgressJobs = mutableMapOf<FileOutputTask,Job>()
    val finishedJobsChannel = Channel<TaskResult>(capacity = CAPACITY_PADDING_FACTOR * PARALLELISM)
    while (unfinishedTasks.get() > 0) {
        check(inProgressJobs.isNotEmpty() || unstartedTasks.isNotEmpty()) {
            "Have ${unfinishedTasks.get()} unfinished tasks, but none are in progress"
        }
        val maybeReceive = finishedJobsChannel.tryReceive().getOrElse<TaskResult?> {
            if (inProgressJobs.isNotEmpty()) {
                if (unstartedTasks.isEmpty()) {
                    logger.debug("{} tasks remain. Waiting for one of: {}",
                        Unbox.box(unfinishedTasks.get()), inProgressJobs)
                    return@getOrElse finishedJobsChannel.receive()
                } else {
                    val currentFree = freeBytes(MEMORY_MX_BEAN.heapMemoryUsage)
                    val lastGcInfo = GC_MX_BEAN.lastGcInfo?.memoryUsageAfterGc?.values?.maxByOrNull { it.max }
                    if (currentFree < MIN_FREE_MEMORY
                        && lastGcInfo != null
                        && freeBytes(lastGcInfo) < MIN_FREE_MEMORY
                    ) {
                        return@getOrElse finishedJobsChannel.receive()
                    } else if (inProgressJobs.size >= PARALLELISM) {
                        yield()
                        return@getOrElse finishedJobsChannel.tryReceive().getOrNull()
                    } else null
                }
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

private fun freeBytes(usage: MemoryUsage): Long = usage.max - usage.used
