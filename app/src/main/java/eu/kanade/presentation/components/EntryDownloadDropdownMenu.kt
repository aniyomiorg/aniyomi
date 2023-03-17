package eu.kanade.presentation.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.entries.DownloadAction
import eu.kanade.tachiyomi.R

@Composable
fun EntryDownloadDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    includeDownloadAllOption: Boolean = true,
    isManga: Boolean,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        val download1 = if (isManga) R.string.download_1 else R.string.download_1_episode
        DropdownMenuItem(
            text = { Text(text = stringResource(download1)) },
            onClick = {
                onDownloadClicked(DownloadAction.NEXT_1_ITEM)
                onDismissRequest()
            },
        )
        val download5 = if (isManga) R.string.download_5 else R.string.download_5_episodes
        DropdownMenuItem(
            text = { Text(text = stringResource(download5)) },
            onClick = {
                onDownloadClicked(DownloadAction.NEXT_5_ITEMS)
                onDismissRequest()
            },
        )
        val download10 = if (isManga) R.string.download_10 else R.string.download_10_episodes
        DropdownMenuItem(
            text = { Text(text = stringResource(download10)) },
            onClick = {
                onDownloadClicked(DownloadAction.NEXT_10_ITEMS)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.download_custom)) },
            onClick = {
                onDownloadClicked(DownloadAction.CUSTOM)
                onDismissRequest()
            },
        )
        val downloadUnviewed = if (isManga) R.string.download_unread else R.string.download_unseen
        DropdownMenuItem(
            text = { Text(text = stringResource(downloadUnviewed)) },
            onClick = {
                onDownloadClicked(DownloadAction.UNVIEWED_ITEMS)
                onDismissRequest()
            },
        )
        if (includeDownloadAllOption) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.download_all)) },
                onClick = {
                    onDownloadClicked(DownloadAction.ALL_ITEMS)
                    onDismissRequest()
                },
            )
        }
    }
}
