package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.OutputTaskEmitter

open class MaterialGroup(private val elements: Sequence<Material>): Material {
    constructor(vararg elements: Material): this(elements.asSequence())

    override fun OutputTaskEmitter.outputTasks(): Unit = elements.forEach {
        it.run { outputTasks() }
    }
}

inline fun <reified E> group(): MaterialGroup where E : Material, E : Enum<E> {
    return MaterialGroup(enumValues<E>().asSequence())
}
