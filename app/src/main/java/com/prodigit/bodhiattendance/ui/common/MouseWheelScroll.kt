
package com.prodigit.bodhiattendance.ui.common

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Mouse wheel scrolling support for scrollable
 * Jetpack Compose content.
 *
 * Use with ScrollState or another ScrollableState.
 */
fun Modifier.mouseWheelScroll(
    scrollState: ScrollableState,
    multiplier: Float = 55f
): Modifier = pointerInput(scrollState, multiplier) {

    coroutineScope {

        awaitPointerEventScope {

            while (true) {

                val event = awaitPointerEvent()

                if (event.type == PointerEventType.Scroll) {

                    val delta = event.changes
                        .firstOrNull()
                        ?.scrollDelta
                        ?.y ?: 0f

                    if (delta != 0f) {

                        launch {
                            scrollState.scrollBy(
                                delta * multiplier
                            )
                        }

                        event.changes.forEach { change ->
                            change.consume()
                        }
                    }
                }
            }
        }
    }
}
