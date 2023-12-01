package eu.kanade.presentation.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import eu.kanade.presentation.entries.DownloadAction
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryDownloadDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    isManga: Boolean,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        val downloadAmount = if (isManga) MR.plurals.download_amount_manga else MR.plurals.download_amount_anime
        val downloadUnviewed = if (isManga) MR.strings.download_unread else MR.strings.download_unseen
        listOfNotNull(
            DownloadAction.NEXT_1_ITEM to pluralStringResource(downloadAmount, 1, 1),
            DownloadAction.NEXT_5_ITEMS to pluralStringResource(downloadAmount, 5, 5),
            DownloadAction.NEXT_10_ITEMS to pluralStringResource(downloadAmount, 10, 10),
            DownloadAction.NEXT_25_ITEMS to pluralStringResource(downloadAmount, 25, 25),
            DownloadAction.UNVIEWED_ITEMS to stringResource(downloadUnviewed),
        ).map { (downloadAction, string) ->
            DropdownMenuItem(
                text = { Text(text = string) },
                onClick = {
                    onDownloadClicked(downloadAction)
                    onDismissRequest()
                },
            )
        }
    }
}
