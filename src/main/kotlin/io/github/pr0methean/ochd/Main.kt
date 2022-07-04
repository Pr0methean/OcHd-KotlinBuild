package io.github.pr0methean.ochd
import io.github.pr0methean.ochd.materials.ALL_MATERIALS
import javafx.embed.swing.JFXPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

suspend fun main(args:Array<String>) {
    lateinit var statsCtx: ImageProcessingContext
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }
    // For some reason, it's faster if we run this line than not, even though JfxTextureTask's ThreadLocal instances
    // already initialize JavaFX on the coroutine threads.
    JFXPanel() // Needed to ensure JFX is initialized

    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")
    val scope = CoroutineScope(Dispatchers.IO)
    val svgDirectory = Paths.get("svg").toAbsolutePath().toFile()
    val metadataDirectory = Paths.get("metadata").toAbsolutePath().toFile()
    println("SVG directory is ${svgDirectory}")
    val out = Paths.get("out").toAbsolutePath().toFile()
    val outTextureRoot = out.resolve("assets").resolve("minecraft").resolve("textures")
    out.deleteRecursively()
    outTextureRoot.mkdirs()
    val pngDirectory = Paths.get("png").toAbsolutePath().toFile()
    pngDirectory.deleteRecursively()
    pngDirectory.mkdirs()
    val ctx = ImageProcessingContext(
        tileSize = tileSize,
        scope = scope,
        svgDirectory = svgDirectory,
        outTextureRoot = outTextureRoot
    )

    // List constants
    /*
val SIMPLE_ORES = listOf("coal", "copper", "iron", "redstone", "gold", "quartz")
val ORES = listOf(SIMPLE_ORES, listOf("lapis", "diamond", "emerald")).flatten()
val COMMAND_BLOCK_TYPES = listOf("command_block", "repeating_command_block", "chain_command_block")
val NORMAL_MUSIC_DISCS = listOf("far", "wait", "strad", "mall", "cat", "pigstep", "mellohi", "13", "blocks", "stal",
        "ward", "5", "otherside", "chirp")
val DISC_LABEL_COLORS = listOf(DYES.values).subList(1, DYES.values.size - 1)
val OXIDATION_STATES = listOf("exposed", "weathered", "oxidized")
 */
    runBlocking(Dispatchers.IO) {
        // Copy over all metadata files
        scope.launch {
            metadataDirectory.walkTopDown().forEach {
                if (it.isDirectory) {
                    out.resolve(it.relativeTo(metadataDirectory)).mkdirs()
                } else {
                    it.copyTo(out.resolve(it.relativeTo(metadataDirectory)))
                }
            }
        }

        ALL_MATERIALS.outputTasks(ctx).forEach { it.run() }
    }
    ctx.printStats()
}