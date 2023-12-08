package eu.kanade.presentation.entries.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DuplicateAnimeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onOpenAnime: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.are_you_sure))
        },
        text = {
            Text(text = stringResource(MR.strings.confirm_add_duplicate_manga))
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onOpenAnime()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_show_anime))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onConfirm()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_add))
                }
            }
        },
    )
}
