package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton

@Composable
fun BottomRightPlayerControls(
    // TODO(customButton)
    // customButton: CustomButtonEntity?,
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        // TODO(customButton)
        // if (customButton != null) {
        //     Box(
        //         modifier = Modifier.padding(end = MaterialTheme.spacing.smaller),
        //     ) {
        //         Button(onClick = {}) {
        //             Text(text = customButton.title)
        //         }
        //         Box(
        //             modifier = Modifier
        //                 .matchParentSize()
        //                 .combinedClickable(
        //                     onClick = customButton::execute,
        //                     onLongClick = customButton::executeLongClick,
        //                     interactionSource = remember { MutableInteractionSource() },
        //                     indication = null,
        //                 ),
        //         )
        //     }
        // }

        if (isPipAvailable) {
            ControlsButton(
                Icons.Default.PictureInPictureAlt,
                onClick = onPipClick,
            )
        }

        ControlsButton(
            Icons.Default.AspectRatio,
            onClick = onAspectClick,
        )
    }
}
