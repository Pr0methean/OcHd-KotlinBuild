package io.github.pr0methean.ochd

import com.sun.management.GarbageCollectorMXBean
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.plus
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.lang.management.ManagementFactory
import java.nio.file.Paths
import java.util.Comparator.comparingInt
import java.util.Comparator.comparingLong
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private const val CAPACITY_PADDING_FACTOR = 2
private val taskOrderComparator = comparingLong(OutputTask::timesFailed)
    .then(comparingInt {it.totalSubtasks - it.startedOrAvailableSubtasks()})
    .then(comparingInt(OutputTask::startedOrAvailableSubtasks).reversed())
private val logger = LogManager.getRootLogger()
private const val PARALLELISM = 2
private const val HUGE_TILE_PARALLELISM = 1
private const val MIN_FREE_MEMORY = 512L*1024*1024
private val gcMxBean = ManagementFactory.getGarbageCollectorMXBeans()[0] as GarbageCollectorMXBean
private const val HEAP_BEAN_NAME = "heap"
private val heapMxBean = ManagementFactory.getMemoryPoolMXBeans().single {
    logger.info("Found MemoryPoolMXBean {}", it.name)
    if (it.name.contains(HEAP_BEAN_NAME)) {
        return@single true
    }
    return@single false
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("UnstableApiUsage", "DeferredResultUnused")
suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")
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
        val tasks = ALL_MATERIALS.outputTasks(ctx).toList()
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
    var maxRetries = 0L
    while (unfinishedTasks.get() > 0) {
        val maybeReceive = finishedJobsChannel.tryReceive().getOrElse {
            if (inProgressJobs.size >= parallelism
                    || (inProgressJobs.isNotEmpty()
                        && (unstartedTasks.isEmpty() || shouldThrottle()))) {
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
        val task = unstartedTasks.minWithOrNull(taskOrderComparator) ?: continue
        val timesFailed = task.timesFailed()
        if (timesFailed > maxRetries) {
            maxRetries = timesFailed
            val cache = task.source.cache
            if (cache is SemiStrongTaskCache<*>) {
                cache.clearPrimaryCache()
            }
        }
        if (!unstartedTasks.remove(task)) {
            throw RuntimeException("Attempted to remove task more than once: $task")
        }
        inProgressJobs[task] = scope.launch {
            logger.info("Joining {}", task)
            val result = task.await()
            if (result.isSuccess) {
                unfinishedTasks.getAndDecrement()
            }
            finishedJobsChannel.send(TaskResult(task, result.isSuccess))
            if (result.isSuccess) {
                logger.info("Joined {} with result of success", task)
                task.source.removeDirectDependentTask(task)
            } else {
                logger.error("Joined {} with an error: {}", task, result.exceptionOrNull()?.message)
                stats.recordRetries(1)
            }
        }
    }
    finishedJobsChannel.close()
}

fun shouldThrottle(): Boolean {
    val usageAfterLastGc = gcMxBean.lastGcInfo.memoryUsageAfterGc
    val heapAfterLastGc = usageAfterLastGc[usageAfterLastGc.keys.single { it.contains(HEAP_BEAN_NAME) }]!!
    if (heapAfterLastGc.max - heapAfterLastGc.used < MIN_FREE_MEMORY) {
        val currentUsage = heapMxBean.usage
        if (currentUsage.max - currentUsage.used < MIN_FREE_MEMORY) {
            logger.warn("Throttling a new task because too little memory is free")
            return true
        }
    }
    return false
}

