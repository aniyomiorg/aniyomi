package eu.kanade.tachiyomi.ui.player.settings.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.i18n.stringResource

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
        titleRes = MR.strings.action_change_intro_length,
        modifier = Modifier.fillMaxWidth(fraction = if (fromPlayer) 0.5F else 0.8F),
        onConfirmRequest = if (fromPlayer) {
            null
        } else {
            {}
        },
        onDismissRequest = {
            updateSkipIntroLength(newLength.toLong())
            onDismissRequest()
        },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            content = {
                WheelTextPicker(
                    modifier = Modifier.align(Alignment.Center),
                    items = remember { 1..255 }.map { stringResource(MR.strings.seconds_short, it) }.toImmutableList(),
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
