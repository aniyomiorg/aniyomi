package eu.kanade.tachiyomi.ui.player.settings.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.WheelTextPicker

@Composable
fun SkipIntroLengthDialog(
    currentSkipIntroLength: Int,
    defaultSkipIntroLength: Int,
    fromPlayer: Boolean,
    updateSkipIntroLength: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var newLength = 0

    PlayerDialog(
        titleRes = R.string.action_change_intro_length,
        hideSystemBars = fromPlayer,
        confirmButton = {
            TextButton(
                onClick = {
                    updateSkipIntroLength(newLength.toLong())
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            content = {
                WheelTextPicker(
                    modifier = Modifier.align(Alignment.Center),
                    texts = remember { 1..255 }.map { stringResource(R.string.seconds_short, it) },
                    onSelectionChanged = { newLength = it + 1 },
                    startIndex = if (currentSkipIntroLength > 0) {
                        currentSkipIntroLength - 1
                    } else {
                        defaultSkipIntroLength
                    },
                )
            },
        )
    }
}
