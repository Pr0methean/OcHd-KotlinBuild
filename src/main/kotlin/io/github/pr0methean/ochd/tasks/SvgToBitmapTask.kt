package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import org.apache.batik.gvt.renderer.StaticRenderer
import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.w3c.dom.Document
import java.awt.Shape
import java.awt.geom.Rectangle2D.Float
import java.awt.image.BufferedImage
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

private val batikTranscoder: ThreadLocal<ToImageTranscoder> = ThreadLocal.withInitial { ToImageTranscoder() }
const val FRAMES_PER_COMMAND_BLOCK_TEXTURE: Int = 4

private fun isCommandBlock(name: String) = name.startsWith("commandBlock") || name.endsWith("4x")
private fun getHeight(name: String, width: Int): Int = if (isCommandBlock(name)) {
    FRAMES_PER_COMMAND_BLOCK_TEXTURE * width
} else width

private val coloredSvgNames = setOf(
    "bed",
    "blastFurnaceHoles",
    "blastFurnaceHoles1",
    "bonemeal",
    "bonemealSmall",
    "bonemealSmallNoBorder",
    "bookShelves",
    "chain",
    "commandBlockChains",
    "commandBlockChains4x",
    "commandBlockGrid",
    "commandBlockGridFront",
    "doorKnob",
    "furnaceFrontLit",
    "loopArrow4x",
    "soulFlameTorch",
    "soulFlameTorchSmall",
    "torchFlame",
    "torchFlameSmall",

)

/** SVG decoder that stores the last image it decoded, rather than passing it to an encoder. */
private class ToImageTranscoder: SVGAbstractTranscoder() {
    private var lastImage: BufferedImage? = null

    fun takeLastImage(): BufferedImage? {
        val lastImage = this.lastImage
        this.lastImage = null
        return lastImage
    }

    @Throws(TranscoderException::class)
    override fun transcode(
        document: Document?,
        uri: String?,
        output: TranscoderOutput?
    ) {
        val renderer = StaticRenderer()
        // Sets up root, curTxf & curAoi
        super.transcode(document, uri, null)

        // prepare the image to be painted
        val w = width.roundToInt()
        val h = height.roundToInt()

        // paint the SVG document using the bridge package
        // create the appropriate renderer
        renderer.updateOffScreen(w, h)
        renderer.transform = curTxf
        renderer.tree = this.root
        this.root = null // We're done with it...
        try {
            // now we are sure that the aoi is the image size
            val raoi: Shape = Float(0f, 0f, width, height)
            // Warning: the renderer's AOI must be in user space
            renderer.repaint(curTxf.createInverse().createTransformedShape(raoi))
            lastImage = renderer.offScreen
        } finally {
            renderer.dispose()
            ctx.dispose()
        }
    }
}

/**
 * Task that loads an SVG file and converts it to a bitmap of a specified size. Doesn't depend on any other task.
 */
@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode")
class SvgToBitmapTask(
    name: String,
    width: Int,
    private val file: File,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext, graph: Graph<AbstractTask<*>, DefaultEdge>
): AbstractImageTask(name, cache, ctx, width, getHeight(name, width), graph) {

    /** SVG import doesn't depend on any other tasks, so this returns an empty list. */
    override val directDependencies: List<AbstractTask<Nothing>> = listOf()

    override fun equals(other: Any?): Boolean {
        return (other === this) || other is SvgToBitmapTask && other.file == file
    }

    override fun computeHashCode(): Int = file.hashCode()

    private val input = TranscoderInput(file.toURI().toString())

    /**
     * Deduplication of SVG tasks is done by the [io.github.pr0methean.ochd.TaskPlanningContext] based on duplicate
     * file names, so we don't need to assimilate any state from another [SvgToBitmapTask].
     */
    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask = this

    override suspend fun perform(): Image {
        ImageProcessingStats.onTaskLaunched("SvgToBitmapTask", name)
        val awtImage = getAwtImage()
        val image = SwingFXUtils.toFXImage(awtImage, createWritableImage())
        awtImage.flush()
        ImageProcessingStats.onTaskCompleted("SvgToBitmapTask", name)
        return image
    }

    override val tiles: Int = if (isCommandBlock(name)) FRAMES_PER_COMMAND_BLOCK_TEXTURE else 1

    suspend fun getAwtImage(): BufferedImage = withContext(batikTranscoder.asContextElement()) {
        val transcoder = batikTranscoder.get()
        transcoder.setTranscodingHints(mapOf(SVGAbstractTranscoder.KEY_WIDTH to width.toFloat()))
        transcoder.transcode(input, null)
        return@withContext transcoder.takeLastImage()!!
    }

    override fun hasColor(): Boolean = coloredSvgNames.contains(name)
}
