package io.github.pr0methean.ochd.tasks

import com.kitfox.svg.SVGUniverse
import com.kitfox.svg.app.beans.SVGIcon
import com.kitfox.svg.app.beans.SVGPanel.AUTOSIZE_STRETCH
import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.packedimage.PackedImage
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.withPermit
import java.awt.Dimension
import java.awt.image.BufferedImage

/**
 * No within-process method seems to work across multiple threads, so shell out to Inkscape
 */
/*
inkscape -w "$SIZE" -h "$SIZE" "$SVG_DIRECTORY/$1.svg" -o "$PNG_DIRECTORY/$1.png" -y 0.0
 */
// svgSalamander doesn't seem to be thread-safe even when loaded in a ThreadLocal<ClassLoader>
val svgLoaderScope = CoroutineScope(newSingleThreadContext("SVG importer thread"))
data class SvgImportTask(
    val shortName: String,
    private val tileSize: Int,
    override val ctx: ImageProcessingContext
)
    : TextureTask(ctx) {
    override val coroutine: Deferred<PackedImage> by lazy {
        runTaskAsync(svgLoaderScope)
    }

    override suspend fun runTask(): PackedImage
            = ctx.memoryContentionSemaphore.withPermit {super.runTask()}

    val file = ctx.svgDirectory.resolve("$shortName.svg")
    override fun toString(): String = "SvgImportTask for $shortName"

    override suspend fun computeImage(): Image {
        val svgUniverse = SVGUniverse()
        @Suppress("DEPRECATION") val svgUri = svgUniverse.loadSVG(file.toURL())
        val icon = SVGIcon()
        icon.svgURI = svgUri
        icon.svgUniverse = svgUniverse
        icon.preferredSize = Dimension(tileSize, tileSize)
        icon.antiAlias = true
        icon.autosize = AUTOSIZE_STRETCH
        return SwingFXUtils.toFXImage(icon.image as BufferedImage, null)
    }
}