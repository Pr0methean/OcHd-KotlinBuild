package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.packedimage.PackedImage
import org.apache.logging.log4j.util.StringBuilderFormattable

interface TextureTask: StringBuilderFormattable {
    fun isComplete(): Boolean

    fun isStarted(): Boolean

    suspend fun getImage(): PackedImage

    fun getImageNow(): PackedImage?
}