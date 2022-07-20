package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.packedimage.PackedImage
import kotlinx.coroutines.Deferred
import org.apache.logging.log4j.util.StringBuilderFormattable

interface TextureTask: StringBuilderFormattable {
    fun launchAsync(): Deferred<PackedImage>
}