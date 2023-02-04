package io.github.pr0methean.ochd

import com.sun.prism.impl.Disposer
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
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.io.File
import java.nio.file.Paths
import java.util.Comparator.comparingDouble
import java.util.Comparator.comparingInt
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap.KeySetView
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val taskOrderComparator = comparingInt<PngOutputTask> {
    if (it.isCacheAllocationFreeOnMargin()) 0 else 1
}.then(comparingDouble<PngOutputTask> {
    runBlocking { it.cacheClearingCoefficient() }
}.reversed())
.then(comparingInt(PngOutputTask::startedOrAvailableSubtasks).reversed())
.then(comparingInt(PngOutputTask::totalSubtasks))

private val logger = LogManager.getRootLogger()

private const val MAX_OUTPUT_TASKS_PER_CPU = 1.75

private const val MIN_TILE_SIZE_FOR_EXPLICIT_GC = 2048

val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

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
                mkdirsedPaths.add(it)
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
    val nCpus = Runtime.getRuntime().availableProcessors() - if (
        // SWPipeline is the software renderer, so its rendering thread needs one CPU
        com.sun.prism.GraphicsPipeline.getPipeline()::class.qualifiedName == "com.sun.prism.sw.SWPipeline"
    ) 1 else 0
    val maxOutputTaskJobs = (MAX_OUTPUT_TASKS_PER_CPU * nCpus).toInt()
    startMonitoring(scope)
    val time = measureNanoTime {
        onTaskLaunched("Build task graph", "Build task graph")
        val tasks = ALL_MATERIALS.outputTasks(ctx).toSet()
        val mkdirs = ioScope.launch {
            cleanupAndCopyMetadata.join()
            tasks.flatMap(PngOutputTask::files)
                .mapNotNull(File::getParentFile)
                .distinct()
                .filter(mkdirsedPaths::add)
                .forEach(File::mkdirs)
        }
        logger.debug("Got deduplicated output tasks")
        val depsBuildTask = scope.launch { tasks.forEach { it.registerRecursiveDependencies() } }
        logger.debug("Launched deps build task")
        depsBuildTask.join()
        mkdirs.join()
        onTaskCompleted("Build task graph", "Build task graph")
        gcIfUsingLargeTiles(tileSize)
        withContext(Dispatchers.Default) {
            runAll(tasks, scope, maxOutputTaskJobs)
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

@Suppress("NestedBlockDepth")
private suspend fun runAll(
    tasks: Collection<PngOutputTask>,
    scope: CoroutineScope,
    maxJobs: Int
) {
    val ioJobs = ConcurrentHashMap.newKeySet<Job>()

    /*
     * If two output tasks are in different weakly-connected components of the task graph, then they're in different
     * connected components. Launching only one connected component at a time minimizes the number of cached images.
     * Launching the smallest ones first means there'll be fewer tasks in the live set when the cache is at its largest
     * and so GC performance will suffer the least possible.
     */
    val connectedComponents = if (tasks.size > maxJobs) {
        tasks.sortedWith(comparingInt(PngOutputTask::cacheableSubtasks))
            .sortedByConnectedComponents()
    } else setOf(tasks.toMutableList())

    val inProgressJobs = HashMap<PngOutputTask,Job>()
    val finishedJobsChannel = Channel<PngOutputTask>(capacity = maxJobs)
    for (connectedComponent in connectedComponents) {
        logger.info("Starting a new connected component of {} output tasks", box(connectedComponent.size))
        while (connectedComponent.isNotEmpty()) {
            clearFinishedJobs(finishedJobsChannel, inProgressJobs, ioJobs)
            val currentInProgressJobs = inProgressJobs.size
            if (currentInProgressJobs + connectedComponent.size <= maxJobs) {
                logger.info(
                    "{} tasks in progress; starting all {} remaining tasks: {}",
                    box(currentInProgressJobs), box(connectedComponent.size),
                            StringBuilder().appendCollection(connectedComponent, "; ")
                )
                connectedComponent.forEach { inProgressJobs[it] = startTask(scope, it, finishedJobsChannel, ioJobs) }
                connectedComponent.clear()
            } else if (currentInProgressJobs >= maxJobs) {
                logger.info("{} tasks in progress; waiting for one to finish", box(currentInProgressJobs))
                inProgressJobs.remove(finishedJobsChannel.receive())
            } else {
                val task = connectedComponent.minWithOrNull(taskOrderComparator)
                checkNotNull(task) { "Could not get an unstarted task" }
                logger.info("{} tasks in progress; starting {}", box(currentInProgressJobs), task)
                inProgressJobs[task] = startTask(scope, task, finishedJobsChannel, ioJobs)
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

private fun List<PngOutputTask>.sortedByConnectedComponents(): List<MutableSet<PngOutputTask>> {
    val components = mutableListOf<MutableSet<PngOutputTask>>()
    sortTask@ for (task in this) {
        val matchingComponents = components.filter {it.any(task::overlapsWith) }
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
    return components.sortedBy(MutableSet<PngOutputTask>::size)
}

private fun clearFinishedJobs(
    finishedJobsChannel: Channel<PngOutputTask>,
    inProgressJobs: HashMap<PngOutputTask, Job>,
    ioJobs: KeySetView<Job, Boolean>
) {
    do {
        val maybeReceive = finishedJobsChannel.tryReceive().getOrNull()?.also(inProgressJobs::remove)
        val finishedIoJobs = ioJobs.removeIf(Job::isCompleted)
    } while (maybeReceive != null || finishedIoJobs)
}

private fun startTask(
    scope: CoroutineScope,
    task: PngOutputTask,
    finishedJobsChannel: Channel<PngOutputTask>,
    ioJobs: MutableSet<in Job>
) = scope.launch {
    try {
        onTaskLaunched("PngOutputTask", task.name)
        val awtImage = if (task.base is SvgToBitmapTask && !task.base.shouldRenderForCaching()) {
            onTaskLaunched("SvgToBitmapTask", task.base.name)
            task.base.getAwtImage().also { onTaskCompleted("SvgToBitmapTask", task.base.name) }
        } else {
            SwingFXUtils.fromFXImage(task.base.await(), null)
        }
        task.base.removeDirectDependentTask(task)
        ioJobs.add(scope.launch {
            logger.info("Starting file write for {}", task.name)
            task.writeToFiles(awtImage).join()
            onTaskCompleted("PngOutputTask", task.name)
        })
        finishedJobsChannel.send(task)
    } catch (t: Throwable) {
        logger.fatal("{} failed", task, t)
        exitProcess(1)
    }
}
