package io.github.pr0methean.ochd.tasks

import com.sun.prism.GraphicsPipeline
import javafx.scene.image.Image
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.StringBuilderFormattable
import org.apache.logging.log4j.util.Unbox.box

private val logger = LogManager.getLogger("ImageTask")
private val resourcePool by lazy {GraphicsPipeline.getDefaultResourceFactory().textureResourcePool}

fun awaitFreeMemory(bytes: Long, name: String) {
    while (!resourcePool.prepareForAllocation(bytes)) {
        logger.warn("Failed to free {} bytes for {}; trying again...", box(bytes), name)
    }
}
interface ImageTask: StringBuilderFormattable, Task<Image> {
    val asPng: Task<ByteArray>
    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask
}