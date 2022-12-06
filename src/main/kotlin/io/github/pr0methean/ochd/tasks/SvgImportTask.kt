package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import org.apache.batik.gvt.renderer.StaticRenderer
import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderException
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.w3c.dom.Document
import java.awt.Shape
import java.awt.geom.Rectangle2D.Float
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.roundToInt

private val batikTranscoder: ThreadLocal<ToImageTranscoder> = ThreadLocal.withInitial { ToImageTranscoder() }
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
        // curTxf.translate(0.5, 0.5);
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

class SvgImportTask(
    name: String,
    private val width: Int,
    private val file: File,
    stats: ImageProcessingStats,
    taskCache: TaskCache<Image>
): AbstractImageTask(name, taskCache, stats) {

    override val directDependencies: List<Task<Nothing>> = listOf() // SVG import doesn't depend on any other tasks

    override fun equals(other: Any?): Boolean {
        return (other === this) || other is SvgImportTask && other.file == file
    }
    private val hashCode by lazy {file.hashCode()}
    private val input = TranscoderInput(file.toURI().toString())

    override fun hashCode(): Int = hashCode

    override suspend fun perform(): Image {
        stats.onTaskLaunched("SvgImportTask", name)
        val image = withContext(batikTranscoder.asContextElement()) {
            val transcoder = batikTranscoder.get()
            transcoder.setTranscodingHints(mapOf(SVGAbstractTranscoder.KEY_WIDTH to width.toFloat()))
            transcoder.transcode(input, null)
            val awtImage = transcoder.takeLastImage()!!
            val image = SwingFXUtils.toFXImage(awtImage, null)
            awtImage.flush()
            image
        }
        stats.onTaskCompleted("SvgImportTask", name)
        return image
    }
}
