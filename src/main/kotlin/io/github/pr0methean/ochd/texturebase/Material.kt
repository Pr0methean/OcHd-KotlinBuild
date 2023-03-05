package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.OutputTaskBuilder

interface Material {
    fun OutputTaskBuilder.outputTasks()
}
