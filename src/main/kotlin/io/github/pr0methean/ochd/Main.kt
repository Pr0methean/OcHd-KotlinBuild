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
import org.apache.logging.log4j.util.StringBuilderFormattable
import org.apache.logging.log4j.util.Unbox.box
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Comparator.comparingDouble
import java.util.Comparator.comparingInt
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap.KeySetView
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime
import kotlin.text.Charsets.UTF_8

const val MIN_OUTPUT_TASK_JOBS: Int = 1
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

private const val HARD_THROTTLING_THRESHOLD = 0.93
private const val EXPLICIT_GC_THRESHOLD = 0.6
private const val FREED_PER_GC_TO_SUPPRESS_EXPLICIT_GC = 0.05
private val gcMxBean = ManagementFactory.getPlatformMXBeans(GarbageCollectorMXBean::class.java).first()
private val memoryMxBean = ManagementFactory.getMemoryMXBean()
private val heapSizeBytes = memoryMxBean.heapMemoryUsage.max.toDouble()
private val hardThrottlingPointBytes = (heapSizeBytes * HARD_THROTTLING_THRESHOLD).toLong()
private val minClearedPerGcBytes = (heapSizeBytes * FREED_PER_GC_TO_SUPPRESS_EXPLICIT_GC).toLong()
private val explicitGcThresholdBytes = (heapSizeBytes * EXPLICIT_GC_THRESHOLD).toLong()
private const val WORKING_BYTES_PER_PIXEL = 50
val nCpus: Int = Runtime.getRuntime().availableProcessors()

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
    val time = measureNanoTime {
        onTaskLaunched("Build task graph", "Build task graph")
        val tasks = ALL_MATERIALS.outputTasks(ctx).toSet()
        val mkdirs = ioScope.launch {
            deleteOldOutputs.join()
            tasks.flatMap(PngOutputTask::files)
                .mapNotNull(File::parentFile)
                .distinct()
                .filter(mkdirsedPaths::add)
                .forEach(File::mkdirs)
        }
        val prereqIoJobs = listOf(mkdirs, copyMetadata)
        logger.debug("Got deduplicated output tasks")
        val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() } }
        logger.debug("Launched deps build task")
        depsBuildTask.join()
        onTaskCompleted("Build task graph", "Build task graph")
        withContext(Dispatchers.Default) {
            val ioJobs = ConcurrentHashMap.newKeySet<Job>()
            val connectedComponents = if (tasks.size > maximumJobsNow(bytesPerTile)) {

                // Output tasks that are in different weakly-connected components don't share any dependencies, so we
                // launch tasks from one component at a time. We start with the small ones so that they'll become
                // unreachable by the time the largest component hits its peak cache size.
                val components = mutableListOf<MutableSet<PngOutputTask>>()
                sortTask@ for (task in tasks.sortedWith(comparingInt(PngOutputTask::cacheableSubtasks))
                ) {
                    val matchingComponents = components.filter { it.any(task::overlapsWith) }
                    logger.debug("{} is connected to: {}", task, matchingComponents)
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
            } else listOf(tasks.toMutableSet())
            var dotFormatOutputJob: Job? = null
            if (tileSize <= MAX_TILE_SIZE_FOR_PRINT_DEPENDENCY_GRAPH) {
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
                clearFinishedJobs(finishedJobsChannel, inProgressJobs, ioJobs)
                logger.info("Starting a new connected component of {} output tasks", box(connectedComponent.size))
                while (connectedComponent.isNotEmpty()) {
                    clearFinishedJobs(finishedJobsChannel, inProgressJobs, ioJobs)
                    val currentInProgressJobs = inProgressJobs.size
                    val maxJobs = maximumJobsNow(bytesPerTile)
                    if (MIN_OUTPUT_TASK_JOBS in maxJobs..currentInProgressJobs) {
                        val delay = measureNanoTime {
                            inProgressJobs.remove(finishedJobsChannel.receive())
                        }
                        logger.warn("Hard-throttled new task for {} ns", box(delay))
                        gcIfNeeded()
                    } else if (currentInProgressJobs + connectedComponent.size <= maxJobs.coerceAtLeast(1)) {
                        logger.info(
                            "{} tasks in progress; starting all {} currently eligible tasks: {}",
                            box(currentInProgressJobs), box(connectedComponent.size), StringBuilderFormattable {
                                it.appendCollection(connectedComponent, "; ")
                            }
                        )
                        connectedComponent.forEach {
                            inProgressJobs[it] = startTask(scope, it, finishedJobsChannel, ioJobs, prereqIoJobs)
                        }
                        connectedComponent.clear()
                    } else if (currentInProgressJobs >= maxJobs.coerceAtLeast(1)) {
                        logger.info("{} tasks in progress; waiting for one to finish", box(currentInProgressJobs))
                        val delay = measureNanoTime {
                            inProgressJobs.remove(finishedJobsChannel.receive())
                        }
                        logger.warn("Waited for tasks in progress to fall below limit for {} ns", box(delay))
                    } else {
                        val task = connectedComponent.minWithOrNull(taskOrderComparator)
                        checkNotNull(task) { "Error finding a new task to start" }
                        logger.info("{} tasks in progress; starting {}", box(currentInProgressJobs), task)
                        inProgressJobs[task] = startTask(scope, task, finishedJobsChannel, ioJobs, prereqIoJobs)
                        check(connectedComponent.remove(task)) { "Attempted to remove task more than once: $task" }
                    }
                }
            }
            logger.info("All jobs started; waiting for {} running jobs to finish", box(inProgressJobs.size))
            while (inProgressJobs.isNotEmpty()) {
                inProgressJobs.remove(finishedJobsChannel.receive())
            }
            logger.info("All jobs done; closing channel")
            finishedJobsChannel.close()
            logger.info("Waiting for remaining IO jobs to finish")
            ioJobs.joinAll()
            logger.info("All IO jobs are finished")
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
private fun gcIfNeeded() {
    // Check if automatic GC is performing poorly. If so, we launch an explicit GC since we know
    // that the last finished job is now unreachable.
    if (gcMxBean.lastGcInfo?.run {
            val bytesUsedAfter = totalBytesInUse(memoryUsageAfterGc)
            bytesUsedAfter >= explicitGcThresholdBytes &&
                    (totalBytesInUse(memoryUsageBeforeGc) - bytesUsedAfter) < minClearedPerGcBytes
        } == true) {
        System.gc()
    }
}

private fun clearFinishedJobs(
    finishedJobsChannel: Channel<PngOutputTask>,
    inProgressJobs: HashMap<PngOutputTask, Job>,
    ioJobs: KeySetView<Job, Boolean>
): Boolean {
    val anyCleared = clearFinishedJobsIteration(finishedJobsChannel, inProgressJobs, ioJobs)
    if (anyCleared) {
        gcIfNeeded()
        clearFinishedJobsIteration(finishedJobsChannel, inProgressJobs, ioJobs)
    }
    return anyCleared
}

private fun clearFinishedJobsIteration(
    finishedJobsChannel: Channel<PngOutputTask>,
    inProgressJobs: HashMap<PngOutputTask, Job>,
    ioJobs: KeySetView<Job, Boolean>
): Boolean {
    var anyCleared = false
    do {
        val maybeReceive = finishedJobsChannel.tryReceive().getOrNull()
        if (maybeReceive != null) {
            anyCleared = true
            inProgressJobs.remove(maybeReceive)
        }
        val finishedIoJobs = ioJobs.removeIf(Job::isCompleted)
    } while (maybeReceive != null || finishedIoJobs)
    return anyCleared
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
            .toInt()
}

private fun totalBytesInUse(memoryUsage: Map<*, MemoryUsage>): Long = memoryUsage.values.sumOf(MemoryUsage::used)
