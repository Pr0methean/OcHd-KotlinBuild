package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.DoubleTallBlock
import io.github.pr0methean.ochd.texturebase.group
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

val stemColor = c(0x4a8f28)
val stemShadow = c(0x266325)
val stemHighlight = c(0x55ab2d)
val DOUBLE_TALL_FLOWERS = group<DoubleTallFlower>()
enum class DoubleTallFlower: DoubleTallBlock {
    /*
    push flowerStemTall ${flower_stem} tallstem1
push flowerStemTallBorder ${flower_stem_h} tallstem2
push flowerStemBottomBorder ${flower_stem_s} tallstem3
out_stack block/sunflower_bottom

push flowerStemShort ${flower_stem} shortstem1
push flowerStemShortBorder ${flower_stem_h} shortstem2
push flowerStemBottomBorder ${flower_stem_s} shortstem3
out_stack block/sunflower_top

push sunflowerPetals ${yellow} sunflower1
push sunflowerPistil ${black} sunflower2
out_stack block/sunflower_front

out_layer sunflowerPetals block/sunflower_back sunflowerBack1
     */
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