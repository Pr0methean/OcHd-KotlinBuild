package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.TextureTask
import javafx.scene.paint.Paint

data class LayerList(val layers: List<TextureTask<*>>, val background: Paint, val ctx: ImageProcessingContext)