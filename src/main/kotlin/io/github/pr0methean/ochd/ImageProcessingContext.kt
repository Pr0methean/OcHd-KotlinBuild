package io.github.pr0methean.ochd

import com.kitfox.svg.SVGUniverse
import io.github.pr0methean.ochd.tasks.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.io.File
import java.util.*

fun color(web: String) = Color.web(web)

fun color(web: String, alpha: Double) = Color.web(web, alpha)

class ImageProcessingContext(
    val tileSize: Int,
    val scope: CoroutineScope,
    val svgDirectory: File,
    val outDirectory: File,
    val pngDirectory: File
) {

    val svg = SVGUniverse()
    val svgTasks = mutableMapOf<String, SvgImportTask>()
    val nameMap = mutableMapOf<String, Deferred<Image>>()
    val taskDedupMap = mutableMapOf<TextureTask, TextureTask>()
    val outputTaskMap = mutableMapOf<String, OutputTask>()
    init {
        svgDirectory.list()!!.forEach { svgFile ->
            svgTasks[svgFile.removeSuffix(".svg")] = SvgImportTask(svgDirectory.resolve(svgFile), svg, tileSize, scope)
        }
    }

    fun deduplicate(task: TextureTask) = taskDedupMap.getOrPut(task) { task }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
        val importTask = SvgImportTask(svgDirectory.resolve("$name.svg"), svg, tileSize, scope)
        val task = if (paint == null) {
            if (alpha == 1.0) importTask else TransparencyTask(importTask, tileSize, alpha, scope)
        } else RepaintTask(paint, importTask, tileSize, alpha, scope)
        return deduplicate(task)
    }

    fun stack(init: LayerList.() -> Unit): TextureTask {
        val layerTasks = LayerList(this)
        layerTasks.init()
        return deduplicate(
            if (layerTasks.size == 1 && layerTasks.background == Color.TRANSPARENT)
                layerTasks[0]
            else
                ImageStackingTask(layerTasks, tileSize, scope))
    }

    fun animate(init: LayerList.() -> Unit): TextureTask {
        val frames = LayerList(this)
        frames.init()
        return deduplicate(AnimationColumnTask(frames, tileSize, scope))
    }

    fun out(name: String, source: TextureTask) = outputTaskMap.computeIfAbsent(name)
            {BasicOutputTask(source, outDirectory.resolve(name.lowercase(Locale.ENGLISH)), scope)}

    fun rename(oldName: String, newName: String): OutputTask {
        val renameTask = RenameOutputTask(outputTaskMap.get(oldName)!!, outDirectory.resolve(newName), scope)
        return outputTaskMap.computeIfAbsent(newName) {renameTask}
    }

    fun copy(oldName: String, newName: String): OutputTask {
        val renameTask = CopyOutputTask(outputTaskMap.get(oldName)!!, outDirectory.resolve(newName), scope)
        return outputTaskMap.computeIfAbsent(newName) {renameTask}
    }
}