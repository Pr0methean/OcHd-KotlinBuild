package io.github.pr0methean.ochd
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import java.nio.file.Paths
import kotlin.system.measureNanoTime

private val logger = run {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    LogManager.getRootLogger()
}
suspend fun main(args:Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    System.setProperty("glass.platform","Monocle")
    System.setProperty("monocle.platform","Headless")

    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")
    val scope = CoroutineScope(Dispatchers.Default)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val metadataDirectory = Paths.get("metadata").toAbsolutePath().toFile()
    val out = Paths.get("out").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")
    out.deleteRecursively()
    outTextureRoot.mkdirs()
    val pngDirectory = Paths.get("png").toAbsolutePath().toFile()
    pngDirectory.deleteRecursively()
    pngDirectory.mkdirs()
    val ctx = ImageProcessingContext(
        name = "MainContext",
        tileSize = tileSize,
        scope = scope,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot
    )
    val stats = ctx.stats
    startMonitoring(stats, scope)
    val time = measureNanoTime {
        val copyMetadata = CoroutineScope(Dispatchers.IO).launch {
            stats.onTaskLaunched("Copying metadata files")
            metadataDirectory.walkTopDown().forEach {
                val outputPath = out.resolve(it.relativeTo(metadataDirectory))
                if (it.isDirectory) {
                    outputPath.mkdirs()
                } else {
                    it.copyTo(outputPath)
                }
            }
            stats.onTaskCompleted("Copying metadata files")
        }}
        stats.onTaskLaunched("Building task graph")
        val tasks = ALL_MATERIALS.outputTasks(ctx).toList()
        stats.onTaskCompleted("Building task graph")
        tasks.map {scope.launch {it.run()}}.joinAll()
        copyMetadata.join()
    }
    stats.log()
    logger.info("")
    logger.info("All tasks finished after $time ns")
}
