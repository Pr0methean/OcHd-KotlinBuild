package io.github.pr0methean.ochd

import com.sun.management.GarbageCollectorMXBean
import io.github.pr0methean.ochd.ImageProcessingStats.onTaskCompleted
import io.github.pr0methean.ochd.ImageProcessingStats.onTaskLaunched
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.tasks.SvgToBitmapTask
import io.github.pr0methean.ochd.tasks.mkdirsedPaths
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Comparator.comparingDouble
import java.util.Comparator.comparingInt
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime
import kotlin.text.Charsets.UTF_8

const val CAPACITY_PADDING_FACTOR: Int = 2
private val taskOrderComparator = comparingInt<PngOutputTask> {
    if (it.isCacheAllocationFreeOnMargin()) 0 else 1
}.then(comparingDouble {
    - runBlocking { it.cacheClearingCoefficient() }
})
.then(comparingInt(PngOutputTask::startedOrAvailableSubtasks).reversed())
.then(comparingInt(PngOutputTask::totalSubtasks))

private val logger = LogManager.getRootLogger()

private const val MAX_TILE_SIZE_FOR_PRINT_DEPENDENCY_GRAPH = 32

val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

private const val FORCE_GC_THRESHOLD = 0.97
private const val HARD_THROTTLING_THRESHOLD = 0.99
private const val EXPLICIT_GC_THRESHOLD = 0.75
private const val FREED_PER_GC_TO_SUPPRESS_EXPLICIT_GC = 0.03
private val gcMxBean = ManagementFactory.getPlatformMXBeans(GarbageCollectorMXBean::class.java).first()
private val memoryMxBean = ManagementFactory.getMemoryMXBean()
private val heapSizeBytes = memoryMxBean.heapMemoryUsage.max.toDouble()
private val hardThrottlingPointBytes = (heapSizeBytes * HARD_THROTTLING_THRESHOLD).toLong()
private val forceGcThresholdBytes = (heapSizeBytes * FORCE_GC_THRESHOLD).toLong()
private val minClearedPerGcBytes = (heapSizeBytes * FREED_PER_GC_TO_SUPPRESS_EXPLICIT_GC).toLong()
private val explicitGcThresholdBytes = (heapSizeBytes * EXPLICIT_GC_THRESHOLD).toLong()
private const val WORKING_BYTES_PER_PIXEL = 48
val nCpus: Int = Runtime.getRuntime().availableProcessors()
private const val MIN_OUTPUT_TASKS = 3

@Suppress("UnstableApiUsage", "DeferredResultUnused", "NestedBlockDepth", "LongMethod", "ComplexMethod")
suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    val tileSize = args[0].toInt()
    require(tileSize > 0) { "tileSize shouldn't be zero or negative but was ${args[0]}" }
    val bytesPerTile = tileSize.toLong() * tileSize * WORKING_BYTES_PER_PIXEL
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
    onTaskLaunched("Build task graph", "Build task graph")
    val dirs = mutableSetOf<File>()
    val tasks = mutableSetOf<PngOutputTask>()
    val outputTaskBuilder = OutputTaskBuilder(ctx) {
        logger.debug("Emitting output task: {}", it)
        tasks.add(it)
        dirs.addAll(it.files.mapNotNull(File::parentFile))
    }
    ALL_MATERIALS.run { outputTaskBuilder.outputTasks() }
    val mkdirs = ioScope.launch {
        deleteOldOutputs.join()
        dirs.filter(mkdirsedPaths::add)
            .forEach(File::mkdirs)
    }
    val prereqIoJobs = listOf(mkdirs, copyMetadata)
    logger.debug("Got deduplicated output tasks")
    val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() } }
    logger.debug("Launched deps build task")
    depsBuildTask.join()
    onTaskCompleted("Build task graph", "Build task graph")
    val dotOutputEnabled = tileSize <= MAX_TILE_SIZE_FOR_PRINT_DEPENDENCY_GRAPH
    withContext<Unit>(Dispatchers.Default) {
        val ioJobs = ConcurrentHashMap.newKeySet<Job>()
        val connectedComponents = if (tasks.size > maximumJobsNow(bytesPerTile) || dotOutputEnabled) {

            // Output tasks that are in different weakly-connected components don't share any dependencies, so we
            // launch tasks from one component at a time. We start with the small ones so that they'll become
            // unreachable by the time the largest component hits its peak cache size.
            val components = mutableListOf<MutableSet<PngOutputTask>>()
            sortTask@ for (task in tasks.sortedWith(comparingInt(PngOutputTask::cacheableSubtasks))
            ) {
                val matchingComponents = components.filter { it.any(task::overlapsWith) }
                logger.debug("{} is connected to: {}", task, matchingComponents.asFormattable())
                if (matchingComponents.isEmpty()) {
                    components.add(mutableSetOf(task))
                } else {
                    matchingComponents.first().add(task)
                    for (component in matchingComponents.drop(1)) {
                        // More than one match = need to merge components
                        matchingComponents.first().addAll(component)
                        check(components.remove(component)) {
                            "Failed to remove $component after merging into ${matchingComponents.first()}"
                        }
                    }
                }
            }
            components.sortedBy(MutableSet<PngOutputTask>::size)
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
        val inProgressJobs = HashMap<PngOutputTask, Job>()
        val finishedJobsChannel = Channel<PngOutputTask>(
            capacity = CAPACITY_PADDING_FACTOR * nCpus
        )
        dotFormatOutputJob?.join()
        for (connectedComponent in connectedComponents) {
            logger.info("Starting a new connected component of {} output tasks", box(connectedComponent.size))
            while (connectedComponent.isNotEmpty()) {
                val currentInProgressJobs = inProgressJobs.size
                val maxJobs = maximumJobsNow(bytesPerTile)
                if (currentInProgressJobs >= maxJobs) {
                    logger.info("{} tasks in progress; waiting for one to finish", box(currentInProgressJobs))
                    val delay = measureNanoTime {
                        inProgressJobs.remove(finishedJobsChannel.receive())
                    }
                    logger.warn("Waited for tasks in progress to fall below limit for {} ns", box(delay))
                    ioJobs.removeIf(Job::isCompleted)
                    gcIfNeeded()
                    continue
                } else if (currentInProgressJobs + connectedComponent.size <= maxJobs) {
                    logger.info(
                        "{} tasks in progress; starting all {} currently eligible tasks: {}",
                        box(currentInProgressJobs), box(connectedComponent.size), connectedComponent.asFormattable()
                    )
                    connectedComponent.forEach {
                        inProgressJobs[it] = startTask(scope, it, finishedJobsChannel, ioJobs, prereqIoJobs)
                    }
                    connectedComponent.clear()
                } else {
                    val task = connectedComponent.minWithOrNull(taskOrderComparator)
                    checkNotNull(task) { "Error finding a new task to start" }
                    logger.info("{} tasks in progress; starting {}", box(currentInProgressJobs), task)
                    inProgressJobs[task] = startTask(scope, task, finishedJobsChannel, ioJobs, prereqIoJobs)
                    check(connectedComponent.remove(task)) { "Attempted to remove task more than once: $task" }
                }
                if (currentInProgressJobs > 0) { // intentionally excludes any jobs started in this iteration
                    // Check for finished tasks before reevaluating the task graph or memory limit
                    var cleared = 0
                    var ioCleared = 0
                    do {
                        val finishedIoJobs = ioJobs.filter(Job::isCompleted).toSet()
                        ioJobs.removeAll(finishedIoJobs)
                        ioCleared += finishedIoJobs.size
                        val maybeReceive = finishedJobsChannel.tryReceive().getOrNull()
                        if (maybeReceive != null) {
                            cleared++
                            inProgressJobs.remove(maybeReceive)
                        }
                    } while (maybeReceive != null || finishedIoJobs.isNotEmpty())
                    logger.info("Collected {} finished tasks and {} finished IO jobs non-blockingly",
                            box(cleared), box(ioCleared))
                    if (cleared > 0 || ioCleared > 0) {
                        gcIfNeeded()
                    }
                }
            }
        }
        logger.info("All jobs started; waiting for {} running jobs to finish", box(inProgressJobs.size))
        while (inProgressJobs.isNotEmpty()) {
            ioJobs.removeIf(Job::isCompleted)
            inProgressJobs.remove(finishedJobsChannel.receive())
        }
        logger.info("All jobs done; closing channel")
        finishedJobsChannel.close()
        ioJobs.removeIf(Job::isCompleted)
        logger.info("Waiting for {} remaining IO jobs to finish", box(ioJobs.size))
        ioJobs.joinAll()
        logger.info("All IO jobs are finished")
    }
    val runningTime = System.nanoTime() - startTime
    stopMonitoring()
    Platform.exit()
    ImageProcessingStats.log()
    logger.info("")
    logger.info("All tasks finished after {} ns", box(runningTime))
    exitProcess(0)
}

/**
 * Checks whether the most recent garbage collection was unsatisfactory and, if so, launches another GC.
 * Should only be called after a task has finished and become unreachable, because that's the only relevant
 * information that the garbage collector's scheduling heuristics don't know and use.
 */
@Suppress("ExplicitGarbageCollectionCall")
private fun gcIfNeeded() {
    val heapUsageNow = memoryMxBean.heapMemoryUsage.used
    // Check if automatic GC is performing poorly or heap is nearly full. If so, we launch an explicit GC since we know
    // that the last finished job is now unreachable.
    if (heapUsageNow > forceGcThresholdBytes) {
        System.gc()
    } else if (heapUsageNow >= explicitGcThresholdBytes && gcMxBean.lastGcInfo?.run {
            val bytesUsedAfter = totalBytesInUse(memoryUsageAfterGc)
            bytesUsedAfter >= explicitGcThresholdBytes
                    && (totalBytesInUse(memoryUsageBeforeGc) - bytesUsedAfter) < minClearedPerGcBytes
        } == true) {
        System.gc()
    }
}

private fun startTask(
    scope: CoroutineScope,
    task: PngOutputTask,
    finishedJobsChannel: Channel<PngOutputTask>,
    ioJobs: MutableSet<in Job>,
    prereqIoJobs: Collection<Job>
) = scope.launch(CoroutineName(task.name)) {
    try {
        onTaskLaunched("PngOutputTask", task.name)
        val awtImage = if (task.base is SvgToBitmapTask && !task.base.shouldRenderForCaching()) {
            onTaskLaunched("SvgToBitmapTask", task.base.name)
            task.base.getAwtImage().also { onTaskCompleted("SvgToBitmapTask", task.base.name) }
        } else {
            SwingFXUtils.fromFXImage(task.base.await(), null)
        }
        task.base.removeDirectDependentTask(task)
        ioJobs.add(scope.launch(CoroutineName("File write for ${task.name}")) {
            prereqIoJobs.joinAll()
            logger.info("Starting file write for {}", task.name)
            task.writeToFiles(awtImage).join()
            onTaskCompleted("PngOutputTask", task.name)
        })
        finishedJobsChannel.send(task)
    } catch (t: Throwable) {
        // Fail fast
        logger.fatal("{} failed", task, t)
        exitProcess(1)
    }
}

fun maximumJobsNow(bytesPerTile: Long): Int {
    return ((hardThrottlingPointBytes - memoryMxBean.heapMemoryUsage.used) / bytesPerTile)
            .toInt().coerceAtLeast(MIN_OUTPUT_TASKS)
}

private fun totalBytesInUse(memoryUsage: Map<*, MemoryUsage>): Long = memoryUsage.values.sumOf(MemoryUsage::used)
