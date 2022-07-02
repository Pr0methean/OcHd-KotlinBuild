package io.github.pr0methean.ochd.tasks

import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.CoroutineScope
import java.io.File
import javax.imageio.ImageIO

data class BasicOutputTask(private val producer: TextureTask, override val file: File, val scope: CoroutineScope)
        : OutputTask(scope, file) {
    override suspend fun invoke() {
        ImageIO.write(SwingFXUtils.fromFXImage(producer.getBitmap(), null), "png", file)
    }
}