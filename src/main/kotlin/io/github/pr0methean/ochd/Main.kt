package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.ImageProcessingStats.cachedTiles
import io.github.pr0methean.ochd.ImageProcessingStats.onCachingEnabled
import io.github.pr0methean.ochd.ImageProcessingStats.onTaskCompleted
import io.github.pr0methean.ochd.ImageProcessingStats.onTaskLaunched
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.AbstractTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.tasks.SvgToBitmapTask
import io.github.pr0methean.ochd.tasks.mkdirsedPaths
import io.github.pr0methean.ochd.tasks.pendingSnapshotTiles
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.graph.DefaultEdge
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Comparator.comparingInt
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.seconds

const val CAPACITY_PADDING_FACTOR: Int = 2
private val taskOrderComparator = comparingInt<PngOutputTask> { if (it.newCacheTiles() > 0) 1 else 0 }
.then(comparingInt(PngOutputTask::removedCacheTiles).reversed())
.then(comparingInt(PngOutputTask::startedOrAvailableSubtasks).reversed())
.then(comparingInt(PngOutputTask::totalSubtasks))

private val logger = LogManager.getRootLogger()

private const val MAX_TILE_SIZE_FOR_PRINT_DEPENDENCY_GRAPH = 32

val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

/**
 * If the pixel array bodies for (images being generated + images in cache) exceed this fraction of the heap, we
 * throttle starting new output tasks.
 */
private const val GOAL_IMAGES_FRACTION_OF_HEAP = 0.43
private val memoryMxBean = ManagementFactory.getMemoryMXBean()
private val heapSizeBytes = memoryMxBean.heapMemoryUsage.max.toDouble()
private val goalImageBytes = heapSizeBytes * GOAL_IMAGES_FRACTION_OF_HEAP
private const val BYTES_PER_PIXEL = 4
val nCpus: Int = Runtime.getRuntime().availableProcessors()
/**
 * The number of tasks per CPU we run when memory-constrained.
 */
private const val MIN_OUTPUT_TASKS_PER_CPU = 1
/**
 * The number of tasks per CPU we run when <em>not</em> memory-constrained.
 */
private const val MAX_OUTPUT_TASKS_PER_CPU = 2
private val maxOutputTasks = nCpus * MAX_OUTPUT_TASKS_PER_CPU
private val minOutputTasks = nCpus * MIN_OUTPUT_TASKS_PER_CPU

private const val BUILD_TASK_GRAPH_TASK_NAME = "Build task graph"

@Suppress("UnstableApiUsage", "DeferredResultUnused", "NestedBlockDepth", "LongMethod", "ComplexMethod",
    "LoopWithTooManyJumpStatements")
suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    val tileSize = args[0].toInt()
    require(tileSize > 0) { "tileSize shouldn't be zero or negative but was ${args[0]}" }
    val bytesPerTile = tileSize.toLong() * tileSize * BYTES_PER_PIXEL
    val goalHeapImages = goalImageBytes / bytesPerTile
    logger.info("Will attempt to keep a maximum of {} images in cache", box(goalHeapImages))
    val ioScope = CoroutineScope(Dispatchers.IO)
    val out = Paths.get("pngout").toAbsolutePath().toFile()
    val metadataDirectory = Paths.get("metadata").toAbsolutePath().toFile()
    val deleteOldOutputs = ioScope.launch(CoroutineName("Delete old outputs")) {
        out.deleteRecursively()
    }
    val copyMetadata = ioScope.launch(CoroutineName("Copy metadata files")) {
        deleteOldOutputs.join()
        metadataDirectory.walkTopDown().forEach {
            val outputPath = out.resolve(it.relativeTo(metadataDirectory))
            if (it.isDirectory && mkdirsedPaths.add(it)) {
                outputPath.mkdirs()
            } else {
                Files.createLink(outputPath.toPath(), it.toPath())
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
    withContext(Dispatchers.Main) {
        Thread.currentThread().priority = Thread.MAX_PRIORITY
    }
    startMonitoring(scope)
    val startTime = System.nanoTime()
    onTaskLaunched(BUILD_TASK_GRAPH_TASK_NAME, BUILD_TASK_GRAPH_TASK_NAME)
    val dirs = mutableSetOf<File>()
    val tasks = mutableSetOf<PngOutputTask>()
    val outputTaskEmitter = OutputTaskEmitter(ctx) {
        logger.debug("Emitting output task: {}", it)
        tasks.add(it)
        dirs.addAll(it.files.mapNotNull(File::parentFile))
    }
    ALL_MATERIALS.run { outputTaskEmitter.outputTasks() }
    val mkdirs = ioScope.launch {
        deleteOldOutputs.join()
        dirs.filter(mkdirsedPaths::add)
            .forEach(File::mkdirs)
    }
    val prereqIoJobs = listOf(mkdirs, copyMetadata)
    logger.debug("Got deduplicated output tasks")
    val depsBuildTask = scope.launch {
        tasks.forEach { it.registerRecursiveDependencies() }
        ctx.graph.vertexSet().forEach {
            if (ctx.graph.inDegreeOf(it) > 1) {
                onCachingEnabled(it)
                it.cache.enable()
            }
        }
    }
    logger.debug("Launched deps build task")
    depsBuildTask.join()
    onTaskCompleted(BUILD_TASK_GRAPH_TASK_NAME, BUILD_TASK_GRAPH_TASK_NAME)
    val dotOutputEnabled = tileSize <= MAX_TILE_SIZE_FOR_PRINT_DEPENDENCY_GRAPH
    val ioJobs = ConcurrentHashMap.newKeySet<Job>()
    val connectedComponents: List<MutableSet<PngOutputTask>> = if (tasks.size > maxOutputTasks || dotOutputEnabled) {
        // Output tasks that are in different weakly-connected components don't share any dependencies, so we
        // launch tasks from one component at a time to keep the number of cached images manageable. We start
        // with the small ones so that they'll become unreachable before the largest component hits its peak cache
        // size, thus limiting the peak size of the live set and reducing the size of heap we need.
        ConnectivityInspector(ctx.graph)
            .connectedSets()
            .sortedBy { it.sumOf(AbstractTask<*>::tiles) }
            .map { it.filterIsInstanceTo(mutableSetOf()) }
    } else listOf(tasks)
    var dotFormatOutputJob: Job? = null
    if (dotOutputEnabled) {
        // Output connected components in .dot format
        dotFormatOutputJob = ioScope.launch {
            @Suppress("BlockingMethodInNonBlockingContext")
            Paths.get("out").toFile().mkdirs()
            Paths.get("out", "graph.dot").toFile().printWriter(UTF_8).use { writer ->
                // Strict because multiedges are possible
                writer.println("strict digraph {")
                writer.println("\"OcHd\" [root=true]")
                connectedComponents.forEachIndexed { index, connectedComponent ->
                    writer.print("subgraph cluster_")
                    writer.print(index)
                    writer.println('{')
                    connectedComponent.forEach {
                        it.printDependencies(writer)
                    }
                    writer.println('}')
                }
                connectedComponents.forEach { connectedComponent ->
                    connectedComponent.forEach {
                        writer.print("\"OcHd\" -> \"")
                        it.appendForGraphPrinting(writer)
                        writer.println("\"")
                        it.printDependencies(writer)
                    }
                }
                writer.println('}')
            }
        }
    }
    val inProgressJobs = ConcurrentHashMap<PngOutputTask, Job>()
    val finishedJobsChannel = Channel<PngOutputTask>(capacity = CAPACITY_PADDING_FACTOR * maxOutputTasks)
    dotFormatOutputJob?.join()
    for (connectedComponent in connectedComponents) {
        logger.info("Starting a new connected component of {} output tasks", box(connectedComponent.size))
        while (connectedComponent.isNotEmpty()) {
            if (inProgressJobs.isNotEmpty()) {
                do {
                    val finishedJob = finishedJobsChannel.tryReceive().getOrNull()
                    if (finishedJob != null) {
                        inProgressJobs.remove(finishedJob)
                    }
                } while (finishedJob != null && inProgressJobs.isNotEmpty())
            }
            val currentInProgressJobs = inProgressJobs.size
            if (currentInProgressJobs >= maxOutputTasks) {
                logger.info("{} tasks in progress; waiting for one to finish", box(currentInProgressJobs))
                val delay = measureNanoTime {
                    finishedJobsChannel.receive()
                }
                logger.warn("Waited for tasks in progress to fall below limit for {} ns", box(delay))
                continue
            } else if (currentInProgressJobs + connectedComponent.size <= minOutputTasks) {
                logger.info(
                    "{} tasks in progress; starting all {} currently eligible tasks: {}",
                    box(currentInProgressJobs), box(connectedComponent.size), connectedComponent.asFormattable()
                )
                connectedComponent.forEach {
                    startTask(
                        scope,
                        it,
                        finishedJobsChannel,
                        ioJobs,
                        prereqIoJobs,
                        inProgressJobs,
                        ctx.graph
                    )
                }
                connectedComponent.clear()
            } else {
                val task = connectedComponent.minWithOrNull(taskOrderComparator)
                checkNotNull(task) { "Error finding a new task to start" }
                if (currentInProgressJobs >= minOutputTasks) {
                    val cachedTiles = cachedTiles()
                    val newTiles = task.newCacheTiles()
                    val impendingTiles = inProgressJobs.keys.sumOf(PngOutputTask::impendingCacheTiles)
                    logger.info(
                        "Cached tiles: {} current, {} impending, {} snapshots, {} when next task starts",
                        box(cachedTiles), box(impendingTiles), box(pendingSnapshotTiles()), box(newTiles))
                    val totalCacheWithThisTask = cachedTiles + impendingTiles + newTiles
                    if (totalCacheWithThisTask >= goalHeapImages && newTiles > 0) {
                        logger.warn("{} tasks in progress and too many tiles cached; waiting for one to finish",
                                currentInProgressJobs)
                        val delay = measureNanoTime {
                            finishedJobsChannel.receive()
                        }
                        logger.warn("Waited for a task to finish for {} ns", box(delay))
                        continue
                    }
                }
                logger.info("{} tasks in progress; starting {}", box(currentInProgressJobs), task)
                startTask(
                    scope,
                    task,
                    finishedJobsChannel,
                    ioJobs,
                    prereqIoJobs,
                    inProgressJobs,
                    ctx.graph
                )
                check(connectedComponent.remove(task)) { "Attempted to remove task more than once: $task" }
            }
        }
    }
    try {
        withTimeout(10.seconds) {
            logger.info("All jobs started; waiting for {} running jobs to finish", box(inProgressJobs.size))
            while (inProgressJobs.isNotEmpty()) {
                inProgressJobs.remove(finishedJobsChannel.receive())
            }
        }
    } catch (e: TimeoutCancellationException) {
        logger.fatal("Jobs still not finished: {}", inProgressJobs.keys.asFormattable(), e)
        exitProcess(1)
    }
    logger.info("All jobs done; closing channel")
    finishedJobsChannel.close()
    check(ctx.graph.vertexSet().isEmpty()) {
        buildString {
            append("Vertices still in graph:")
            append(System.lineSeparator())
            appendFormattables(ctx.graph.vertexSet())
        }
    }
    logger.info("Waiting for {} remaining IO jobs to finish", box(ioJobs.size))
    ioJobs.joinAll()
    logger.info("All IO jobs are finished")
    val runningTime = System.nanoTime() - startTime
    stopMonitoring()
    Platform.exit()
    ImageProcessingStats.finalChecks()
    logger.info("")
    logger.info("All tasks finished after {} ns", box(runningTime))
    exitProcess(0)
}

@Suppress("LongParameterList","TooGenericExceptionCaught")
private fun startTask(
    scope: CoroutineScope,
    task: PngOutputTask,
    finishedJobsChannel: Channel<PngOutputTask>,
    ioJobs: MutableSet<in Job>,
    prereqIoJobs: Collection<Job>,
    inProgressJobs: ConcurrentMap<PngOutputTask, Job>,
    graph: Graph<AbstractTask<*>, DefaultEdge>
) {
    inProgressJobs[task] = scope.launch(CoroutineName(task.name)) {
        try {
            onTaskLaunched("PngOutputTask", task.name)
            val awtImage = if (task.base is SvgToBitmapTask && !task.base.shouldRenderForCaching()) {
                onTaskLaunched("SvgToBitmapTask", task.base.name)
                task.base.getAwtImage().also { onTaskCompleted("SvgToBitmapTask", task.base.name) }
            } else {
                SwingFXUtils.fromFXImage(task.base.await(), null)
            }
            task.base.removeDirectDependentTask(task)
            graph.removeVertex(task)
            val ioJob = scope.launch(CoroutineName("File write for ${task.name}")) {
                prereqIoJobs.joinAll()
                logger.info("Starting file write for {}", task.name)
                task.writeToFiles(awtImage).join()
                onTaskCompleted("PngOutputTask", task.name)
            }
            ioJob.invokeOnCompletion { ioJobs.remove(ioJob) }
            ioJobs.add(ioJob)
            inProgressJobs.remove(task)
            finishedJobsChannel.send(task)
        } catch (t: Throwable) {
            // Fail fast
            logger.fatal("{} failed", task, t)
            exitProcess(1)
        }
    }
}
