package io.github.pr0methean.ochd

import com.sun.prism.impl.Disposer
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.PngOutputTask
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.nio.file.Paths
import java.util.Comparator.comparingInt
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private const val CAPACITY_PADDING_FACTOR = 2
private val taskOrderComparator = comparingInt<PngOutputTask> { runBlocking { it.netAddedToCache() } }
    .then(comparingInt(PngOutputTask::startedOrAvailableSubtasks).reversed())
    .then(comparingInt(PngOutputTask::totalSubtasks))
private val logger = LogManager.getRootLogger()
private const val THREADS_PER_CPU = 1.0
private val THREADS = perCpu(THREADS_PER_CPU)
private const val MAX_OUTPUT_TASKS_PER_CPU = 3.0
private val MAX_OUTPUT_TASKS = perCpu(MAX_OUTPUT_TASKS_PER_CPU)
private const val MAX_HUGE_TILE_OUTPUT_TASKS_PER_CPU = 1.5
private val MAX_HUGE_TILE_OUTPUT_TASKS = perCpu(MAX_HUGE_TILE_OUTPUT_TASKS_PER_CPU)
private const val MIN_TILE_SIZE_FOR_EXPLICIT_GC = 2048

private fun perCpu(amount: Double) = (amount * Runtime.getRuntime().availableProcessors()).toInt()

@OptIn(DelicateCoroutinesApi::class)
@Suppress("UnstableApiUsage", "DeferredResultUnused")
suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    val tileSize = args[0].toInt()
    require(tileSize > 0) { "tileSize shouldn't be zero or negative but was ${args[0]}" }
    val ioScope = CoroutineScope(Dispatchers.IO)
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
    val scope = CoroutineScope(coroutineContext)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")

    val ctx = TaskPlanningContext(
        name = "MainContext",
        tileSize = tileSize,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot,
        ctx = coroutineContext
    )
    scope.launch {
        withContext(Dispatchers.Main) {
            Thread.currentThread().priority = Thread.MAX_PRIORITY
        }
    }
    startMonitoring(scope)
    val time = measureNanoTime {
        ImageProcessingStats.onTaskLaunched("Build task graph", "Build task graph")
        val tasks = ALL_MATERIALS.outputTasks(ctx).toSet()
        logger.debug("Got deduplicated output tasks")
        val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() } }
        logger.debug("Launched deps build task")
        val (cbTasks, nonCbTasks) = tasks.partition(PngOutputTask::isCommandBlock)
        depsBuildTask.join()
        ImageProcessingStats.onTaskCompleted("Build task graph", "Build task graph")
        cleanupAndCopyMetadata.join()
        gcIfUsingLargeTiles(tileSize)
        runAll(cbTasks, scope, MAX_HUGE_TILE_OUTPUT_TASKS)
        gcIfUsingLargeTiles(tileSize)
        runAll(nonCbTasks, scope, MAX_OUTPUT_TASKS)
    }
    stopMonitoring()
    Platform.exit()
    ImageProcessingStats.log()
    logger.info("")
    logger.info("All tasks finished after {} ns", Unbox.box(time))
    exitProcess(0)
}

@Suppress("ExplicitGarbageCollectionCall")
private suspend fun gcIfUsingLargeTiles(tileSize: Int) {
    if (tileSize >= MIN_TILE_SIZE_FOR_EXPLICIT_GC) {
        withContext(Dispatchers.Main) {
            Disposer.cleanUp()
        }
        System.gc()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun runAll(
    tasks: Iterable<PngOutputTask>,
    scope: CoroutineScope,
    maxJobs: Int
) {
    val unstartedTasks = tasks.sortedWith(comparingInt(PngOutputTask::cacheableSubtasks)).toMutableSet()
    val inProgressJobs = HashMap<PngOutputTask,Job>()
    val finishedJobsChannel = Channel<PngOutputTask>(capacity = CAPACITY_PADDING_FACTOR * THREADS)
    while (unstartedTasks.isNotEmpty()) {
        do {
            val maybeReceive = finishedJobsChannel.tryReceive().getOrNull()?.also(inProgressJobs::remove)
        } while (maybeReceive != null)
        val currentInProgressJobs = inProgressJobs.size
        val task = unstartedTasks.minWithOrNull(taskOrderComparator)
        checkNotNull(task) { "Could not get an unstarted task" }
        if (currentInProgressJobs < maxJobs || task.netAddedToCache() < 0) {
            inProgressJobs[task] = scope.launch {
                logger.info("Joining {}", task)
                try {
                    task.perform()
                } catch (t: Throwable) {
                    logger.fatal("{} failed", task, t)
                    exitProcess(1)
                }
                finishedJobsChannel.send(task)
            }
            check(unstartedTasks.remove(task)) { "Attempted to remove task more than once: $task" }
        } else {
            inProgressJobs.remove(finishedJobsChannel.receive())
            if (finishedJobsChannel.isEmpty) {
                yield()
            }
        }
    }
    logger.debug("All jobs started; waiting for {} running jobs to finish", Unbox.box(inProgressJobs.size))
    while (inProgressJobs.isNotEmpty()) {
        inProgressJobs.remove(finishedJobsChannel.receive())
    }
    logger.debug("All jobs done; closing channel")
    finishedJobsChannel.close()
}
