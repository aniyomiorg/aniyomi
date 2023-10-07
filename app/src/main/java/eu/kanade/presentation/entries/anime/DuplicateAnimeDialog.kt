package eu.kanade.presentation.entries.anime

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R

@Composable
fun DuplicateAnimeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onOpenAnime: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row {
                TextButton(onClick = {
                    onDismissRequest()
                    onOpenAnime()
                },) {
                    Text(text = stringResource(R.string.action_show_anime))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.action_cancel))
                }
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onConfirm()
                    },
                ) {
                    Text(text = stringResource(R.string.action_add))
                }
            }
        },
        title = {
            Text(text = stringResource(R.string.are_you_sure))
        },
        text = {
            Text(text = stringResource(R.string.confirm_add_duplicate_manga))
        },
    )
}
