package io.github.pr0methean.ochd.tasks

import com.sun.prism.GraphicsPipeline
import javafx.scene.image.Image
import kotlinx.coroutines.yield
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.StringBuilderFormattable
import org.apache.logging.log4j.util.Unbox.box

private val logger = LogManager.getLogger("ImageTask")

suspend fun awaitFreeMemory(bytes: Long, name: String) {
    while (!GraphicsPipeline.getDefaultResourceFactory().textureResourcePool.prepareForAllocation(bytes)) {
        logger.warn("Failed to free {} bytes for {}; trying again...", box(bytes), name)
        yield()
    }
}
interface ImageTask: StringBuilderFormattable, Task<Image> {
    val asPng: Task<ByteArray>
    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask

    suspend fun awaitFreeMemory(bytes: Long): Unit = awaitFreeMemory(bytes, name)
}