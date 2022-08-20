package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.DoubleTallBlock
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

val stemColor = c(0x4a8f28)
val stemShadow = c(0x256325)
val stemHighlight = c(0x55ab2d)

@Suppress("unused")
enum class DoubleTallFlower: DoubleTallBlock {
    SUNFLOWER {
        override suspend fun LayerListBuilder.createBottomLayers() {
            layer("flowerStemTall", stemColor)
            layer("flowerStemTallBorder", stemHighlight)
            layer("flowerStemBottomBorder", stemShadow)
        }

        override suspend fun LayerListBuilder.createTopLayers() {
            layer("flowerStemShort", stemColor)
            layer("flowerStemShortBorder", stemHighlight)
            layer("flowerStemBottomBorder", stemShadow)
        }

        override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = merge(super.outputTasks(ctx), flow {
            emit(ctx.out(ctx.layer("sunflowerPetals"), "block/sunflower_back"))
            emit(ctx.out(ctx.stack {
                layer("sunflowerPetals", Color.YELLOW)
                layer("sunflowerPistil")
            }, "block/sunflower_front"))
        })
    };
}