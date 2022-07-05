package io.github.pr0methean.ochd.tasks

import com.kitfox.svg.SVGUniverse
import com.kitfox.svg.app.beans.SVGIcon
import com.kitfox.svg.app.beans.SVGPanel.AUTOSIZE_STRETCH
import io.github.pr0methean.ochd.ImageProcessingContext
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.image.BufferedImage

/**
 * No within-process method seems to work across multiple threads, so shell out to Inkscape
 */
/*
inkscape -w "$SIZE" -h "$SIZE" "$SVG_DIRECTORY/$1.svg" -o "$PNG_DIRECTORY/$1.png" -y 0.0
 */
data class SvgImportTask(
    val shortName: String,
    private val svg: SVGUniverse,
    private val tileSize: Int,
    override val ctx: ImageProcessingContext
)
    : TextureTask<BufferedImage>(ctx) {
    val file = ctx.svgDirectory.resolve("$shortName.svg")
    override fun toString(): String = "SvgImportTask for $shortName"

    override suspend fun computeInput(): BufferedImage {
        @Suppress("DEPRECATION") val svgUri = withContext(Dispatchers.IO) {
            svg.loadSVG(file.toURL())
        }
        val icon = SVGIcon()
        icon.svgURI = svgUri
        icon.svgUniverse = svg
        icon.preferredSize = Dimension(tileSize, tileSize)
        icon.antiAlias = true
        icon.autosize = AUTOSIZE_STRETCH
        return icon.image as BufferedImage
    }

    override fun doBlockingJfx(input: BufferedImage): Image = SwingFXUtils.toFXImage(input, null)
}