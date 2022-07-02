package io.github.pr0methean.ochd.tasks

import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

data class SvgImportTask(private val filename: File, private val transcoder: ImageTranscoder, private val scope: CoroutineScope)
    : TextureTask(scope) {
    override suspend fun computeBitmap(): Image {
        val svgURI: String = filename.toURI().toURL().toString()
        val input = TranscoderInput(svgURI)
        return ByteArrayOutputStream().use { stream ->
            transcoder.transcode(input, TranscoderOutput(stream))
            ByteArrayInputStream(stream.toByteArray()).use { Image(it) }
        }
    }
}