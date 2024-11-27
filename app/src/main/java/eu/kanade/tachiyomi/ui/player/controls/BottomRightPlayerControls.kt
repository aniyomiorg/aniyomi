package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BottomRightPlayerControls(
    // TODO(customButton)
    // customButton: CustomButtonEntity?,
    aniskipButton: String?,
    onPressAniSkipButton: () -> Unit,
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        if (aniskipButton != null) {
            Box(
                modifier = Modifier.padding(end = MaterialTheme.padding.small),
            ) {
                Button(onClick = {}) {
                    Text(text = aniskipButton)
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .combinedClickable(
                            onClick = onPressAniSkipButton,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                )
            }
        }
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
