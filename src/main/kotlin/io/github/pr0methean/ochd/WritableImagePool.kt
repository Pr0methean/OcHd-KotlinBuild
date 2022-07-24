package io.github.pr0methean.ochd

import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class WritableImagePool(width: Int, height: Int, capacity: Int, scopeForInit: CoroutineScope) {
    private val channel = Channel<WritableImage>(capacity)
    init {
        scopeForInit.launch {repeat(capacity) {channel.send(WritableImage(width, height))}}
    }
    suspend fun <T> borrow(block: suspend (WritableImage) -> T): T {
        val borrowed = channel.receive()
        try {
            return block(borrowed)
        } finally {
            channel.send(borrowed)
        }
    }
}