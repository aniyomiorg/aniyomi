package eu.kanade.presentation.entries

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.presentation.core.components.WheelTextPicker

@Composable
fun DeleteItemsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    isManga: Boolean,
) {
    val subtitle = if (isManga) R.string.confirm_delete_chapters else R.string.confirm_delete_episodes
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = stringResource(R.string.action_ok))
            }
        },
        title = {
            Text(text = stringResource(R.string.are_you_sure))
        },
        text = {
            Text(text = stringResource(subtitle))
        },
    )
}

@Composable
fun SetIntervalDialog(
    interval: Int,
    onDismissRequest: () -> Unit,
    onValueChanged: (Int) -> Unit,
) {
    var selectedInterval by rememberSaveable { mutableIntStateOf(if (interval < 0) -interval else 0) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.manga_modify_calculated_interval_title)) },
        text = {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val size = DpSize(width = maxWidth / 2, height = 128.dp)
                val items = (0..28)
                    .map {
                        if (it == 0) {
                            stringResource(R.string.label_default)
                        } else {
                            it.toString()
                        }
                    }
                    .toImmutableList()
                WheelTextPicker(
                    items = items,
                    size = size,
                    startIndex = selectedInterval,
                    onSelectionChanged = { selectedInterval = it },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueChanged(selectedInterval)
                onDismissRequest()
            }) {
                Text(text = stringResource(R.string.action_ok))
            }
        },
    )
}
