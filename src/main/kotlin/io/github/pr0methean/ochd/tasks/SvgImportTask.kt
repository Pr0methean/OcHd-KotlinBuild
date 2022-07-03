package io.github.pr0methean.ochd.tasks

import com.kitfox.svg.SVGUniverse
import com.kitfox.svg.app.beans.SVGIcon
import com.kitfox.svg.app.beans.SVGPanel.AUTOSIZE_STRETCH
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File

/**
 * No within-process method seems to work across multiple threads, so shell out to Inkscape
 */
/*
inkscape -w "$SIZE" -h "$SIZE" "$SVG_DIRECTORY/$1.svg" -o "$PNG_DIRECTORY/$1.png" -y 0.0
 */
data class SvgImportTask(private val filename: File, private val svg: SVGUniverse, private val tileSize: Int, override val scope: CoroutineScope)
    : JfxTextureTask<BufferedImage>(scope) {

    override suspend fun computeInput(): BufferedImage {
        val icon = SVGIcon()
        icon.svgUniverse = svg
        icon.svgURI = svg.loadSVG(filename.toURL())
        icon.preferredSize = Dimension(tileSize, tileSize)
        icon.antiAlias = true
        icon.autosize = AUTOSIZE_STRETCH
        return icon.image as BufferedImage
    }

    override fun doBlockingJfx(input: BufferedImage): Image = SwingFXUtils.toFXImage(input, null)
}