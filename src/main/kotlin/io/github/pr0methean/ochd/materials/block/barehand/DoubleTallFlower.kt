package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.DoubleTallBlock
import javafx.scene.paint.Color

val stemColor: Color = c(0x4a8f28)
val stemShadow: Color = c(0x256325)
val stemHighlight: Color = c(0x55ab2d)

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

        override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = super.outputTasks(ctx)
                .plus(ctx.out("block/sunflower_back", "sunflowerPetals"))
                .plus(ctx.out("block/sunflower_front") {
                        layer("sunflowerPetals", Color.YELLOW)
                        layer("sunflowerPistil")
                    })
    };
}
