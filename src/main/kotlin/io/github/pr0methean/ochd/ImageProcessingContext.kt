package io.github.pr0methean.ochd

import com.google.common.collect.ConcurrentHashMultiset
import com.kitfox.svg.SVGUniverse
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

fun color(web: String) = Color.web(web)

fun color(web: String, alpha: Double) = Color.web(web, alpha)

class ImageProcessingContext(
    val tileSize: Int,
    val scope: CoroutineScope,
    val svgDirectory: File,
    val outTextureRoot: File
) {

    val svg = SVGUniverse()
    val svgTasks = ConcurrentHashMap<String, SvgImportTask>()
    val taskDedupMap = ConcurrentHashMap<TextureTask, TextureTask>()
    val outputTaskMap = ConcurrentHashMap<String, OutputTask>()
    val taskLaunches: ConcurrentHashMultiset<KClass<*>> = ConcurrentHashMultiset.create<KClass<*>>()
    val dedupeSuccesses: ConcurrentHashMultiset<KClass<*>> = ConcurrentHashMultiset.create<KClass<*>>()
    val dedupeFailures: ConcurrentHashMultiset<KClass<*>> = ConcurrentHashMultiset.create<KClass<*>>()
    init {
        svgDirectory.list()!!.forEach { svgFile ->
            svgTasks[svgFile.removeSuffix(".svg")] = SvgImportTask(
                svgDirectory.resolve(svgFile),
                svg,
                tileSize,
                scope,
                this
            )
        }
    }

    fun printStats() {
        println()
        println("Task launches:")
        taskLaunches.toSet().forEach {println("$it: ${taskLaunches.count(it)}")}
        println()
        println("Deduplication successes:")
        dedupeSuccesses.toSet().forEach {println("$it: ${dedupeSuccesses.count(it)}")}
        println()
        println("Deduplication failures:")
        dedupeFailures.toSet().forEach {println("$it: ${dedupeFailures.count(it)}")}
    }

    fun deduplicate(task: TextureTask): TextureTask {
        dedupeSuccesses.add(task::class)
        return taskDedupMap.computeIfAbsent(task) {
            dedupeSuccesses.remove(task::class)
            dedupeFailures.add(task::class)
            task
        }
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
        val importTask = SvgImportTask(svgDirectory.resolve("$name.svg"), svg, tileSize, scope, this)
        val task = if (paint == null) {
            if (alpha == 1.0) importTask else TransparencyTask(importTask, tileSize, alpha, scope, this)
        } else RepaintTask(paint, importTask, tileSize, alpha, scope, this)
        return deduplicate(task)
    }

    fun stack(init: LayerList.() -> Unit): TextureTask {
        val layerTasks = LayerList(this)
        layerTasks.init()
        return deduplicate(
            if (layerTasks.size == 1 && layerTasks.background == Color.TRANSPARENT)
                layerTasks[0]
            else
                ImageStackingTask(layerTasks, tileSize, scope, this))
    }

    fun animate(init: LayerList.() -> Unit): TextureTask {
        val frames = LayerList(this)
        frames.init()
        return deduplicate(AnimationColumnTask(frames, tileSize, scope, this))
    }

    fun out(name: String, source: TextureTask) = outputTaskMap.computeIfAbsent(name)
            {BasicOutputTask(source, outTextureRoot.resolve(name.lowercase(Locale.ENGLISH) + ".png"), scope, this)}

    fun out(name: String, source: LayerList.() -> Unit) = outputTaskMap.computeIfAbsent(name)
            {BasicOutputTask(
                stack {source()},
                outTextureRoot.resolve(name.lowercase(Locale.ENGLISH) + ".png"),
                scope,
                this
            )}


    fun copy(oldName: String, newName: String): OutputTask {
        val renameTask = CopyOutputTask(outputTaskMap.get(oldName)!!, outTextureRoot.resolve(newName), scope, this)
        return outputTaskMap.computeIfAbsent(newName) {renameTask}
    }
}