package io.github.pr0methean.ochd.tasks.consumable

import javafx.scene.image.Image
import org.apache.logging.log4j.util.StringBuilderFormattable

interface ImageTask: StringBuilderFormattable, Task<Image> {
    val asPng: Task<ByteArray>
}