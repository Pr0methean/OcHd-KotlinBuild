package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.DoubleTallBlock
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

val stemColor = c(0x4a8f28)
val stemShadow = c(0x266325)
val stemHighlight = c(0x55ab2d)

@Suppress("unused")
enum class DoubleTallFlower: DoubleTallBlock {
    SUNFLOWER {
        override fun LayerListBuilder.createBottomLayers() {
            layer("flowerStemTall", stemColor)
            layer("flowerStemTallBorder", stemHighlight)
            layer("flowerStemBottomBorder", stemShadow)
        }

        override fun LayerListBuilder.createTopLayers() {
            layer("flowerStemShort", stemColor)
            layer("flowerStemShortBorder", stemHighlight)
            layer("flowerStemBottomBorder", stemShadow)
        }

        override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = merge(super.outputTasks(ctx), flow {
            emit(ctx.out("block/sunflower_back", ctx.layer("sunflowerPetals")))
            emit(ctx.out("block/sunflower_front", ctx.stack {
                layer("sunflowerPetals", Color.YELLOW)
                layer("sunflowerPistil")
            }))
        })
    };
}