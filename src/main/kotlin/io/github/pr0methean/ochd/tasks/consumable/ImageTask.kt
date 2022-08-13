package io.github.pr0methean.ochd.tasks.consumable

import javafx.scene.image.Image
import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.util.StringBuilderFormattable

interface ImageTask: StringBuilderFormattable, Task<Image> {

    val asPng: Task<ByteArray>
    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask

    @Suppress("DeferredResultUnused")
    override suspend fun startAsync(): Deferred<Result<Image>>
}