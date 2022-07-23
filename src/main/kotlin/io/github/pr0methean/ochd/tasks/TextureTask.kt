package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.packedimage.ImageNode
import org.apache.logging.log4j.util.StringBuilderFormattable

interface TextureTask: StringBuilderFormattable {
    fun isComplete(): Boolean

    fun isStarted(): Boolean

    suspend fun getImage(): ImageNode

    fun getImageNow(): ImageNode?
}