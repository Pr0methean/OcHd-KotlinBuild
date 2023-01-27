package io.github.pr0methean.ochd

import com.sun.prism.impl.Disposer
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.PngOutputTask
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.nio.file.Paths
import java.util.Comparator.comparingDouble
import java.util.Comparator.comparingInt
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private const val CAPACITY_PADDING_FACTOR = 2
private val taskOrderComparator = comparingDouble<PngOutputTask> {
        runBlocking { it.cacheClearingCoefficient() }
    }.reversed()
    .then(comparingInt(PngOutputTask::startedOrAvailableSubtasks).reversed())
    .then(comparingInt(PngOutputTask::totalSubtasks))
private val logger = LogManager.getRootLogger()
private const val THREADS_PER_CPU = 1.0
private val THREADS = perCpu(THREADS_PER_CPU)
private const val MAX_OUTPUT_TASKS_PER_CPU = 2.0
private val MAX_OUTPUT_TASKS = perCpu(MAX_OUTPUT_TASKS_PER_CPU)
private const val MAX_HUGE_TILE_OUTPUT_TASKS_PER_CPU = 3.0
private val MAX_HUGE_TILE_OUTPUT_TASKS = perCpu(MAX_HUGE_TILE_OUTPUT_TASKS_PER_CPU)
private const val MIN_TILE_SIZE_FOR_EXPLICIT_GC = 2048
val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

private fun perCpu(amount: Double) = (amount * Runtime.getRuntime().availableProcessors()).toInt()

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
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")

    val ctx = TaskPlanningContext(
        name = "MainContext",
        tileSize = tileSize,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot,
        ctx = Dispatchers.Default
    )
    scope.plus(Dispatchers.Main).launch {
        Thread.currentThread().priority = Thread.MAX_PRIORITY
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
        withContext(Dispatchers.Default) {
            runAll(cbTasks, scope, MAX_HUGE_TILE_OUTPUT_TASKS)
            gcIfUsingLargeTiles(tileSize)
            runAll(nonCbTasks, scope, MAX_OUTPUT_TASKS)
        }
    }
    stopMonitoring()
    Platform.exit()
    ImageProcessingStats.log()
    logger.info("")
    logger.info("All tasks finished after {} ns", box(time))
    exitProcess(0)
}

@Suppress("ExplicitGarbageCollectionCall")
private fun gcIfUsingLargeTiles(tileSize: Int) {
    if (tileSize >= MIN_TILE_SIZE_FOR_EXPLICIT_GC) {
        System.gc()
        scope.plus(Dispatchers.Main).launch {
            Disposer.cleanUp()
        }
    }
}

private suspend fun runAll(
    tasks: Collection<PngOutputTask>,
    scope: CoroutineScope,
    maxJobs: Int
) {
    val unstartedTasks = if (tasks.size > maxJobs) {
        tasks.sortedWith(comparingInt(PngOutputTask::cacheableSubtasks)).toMutableSet()
    } else tasks.toMutableSet()
    val inProgressJobs = HashMap<PngOutputTask,Job>()
    val finishedJobsChannel = Channel<PngOutputTask>(capacity = CAPACITY_PADDING_FACTOR * THREADS)
    while (unstartedTasks.isNotEmpty()) {
        do {
            val maybeReceive = finishedJobsChannel.tryReceive().getOrNull()?.also(inProgressJobs::remove)
        } while (maybeReceive != null)
        val currentInProgressJobs = inProgressJobs.size
        if (currentInProgressJobs + unstartedTasks.size <= maxJobs) {
            logger.info("{} tasks in progress; starting all {} remaining tasks",
                box(currentInProgressJobs), box(unstartedTasks.size))
            unstartedTasks.forEach { inProgressJobs[it] = startTask(scope, it, finishedJobsChannel) }
            unstartedTasks.clear()
        } else if (currentInProgressJobs < maxJobs) {
            val task = unstartedTasks.minWithOrNull(taskOrderComparator)
            checkNotNull(task) { "Could not get an unstarted task" }
            logger.info("{} tasks in progress; starting {}", box(currentInProgressJobs), task)
            inProgressJobs[task] = startTask(scope, task, finishedJobsChannel)
            check(unstartedTasks.remove(task)) { "Attempted to remove task more than once: $task" }
        } else {
            logger.info("{} tasks in progress; waiting for one to finish", box(currentInProgressJobs))
            inProgressJobs.remove(finishedJobsChannel.receive())
        }
    }
    logger.info("All jobs started; waiting for {} running jobs to finish", box(inProgressJobs.size))
    while (inProgressJobs.isNotEmpty()) {
        inProgressJobs.remove(finishedJobsChannel.receive())
    }
    logger.info("All jobs done; closing channel")
    finishedJobsChannel.close()
}

private fun startTask(
    scope: CoroutineScope,
    task: PngOutputTask,
    finishedJobsChannel: Channel<PngOutputTask>
) = scope.launch {
    try {
        ImageProcessingStats.onTaskLaunched("PngOutputTask", task.name)
        val baseImage = task.base.await()
        finishedJobsChannel.send(task)
        task.writeToFiles(baseImage)
        ImageProcessingStats.onTaskCompleted("PngOutputTask", task.name)
    } catch (t: Throwable) {
        logger.fatal("{} failed", task, t)
        exitProcess(1)
    }
}
