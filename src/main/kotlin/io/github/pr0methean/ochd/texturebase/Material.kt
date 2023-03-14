package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.OutputTaskEmitter

interface Material {
    fun OutputTaskEmitter.outputTasks()
}
