package eu.kanade.presentation.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entries.DownloadAction
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryDownloadDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    isManga: Boolean,
    modifier: Modifier = Modifier,
) {
    val downloadAmount = if (isManga) MR.plurals.download_amount else AYMR.plurals.download_amount_anime
    val downloadUnviewed = if (isManga) MR.strings.download_unread else AYMR.strings.download_unseen
    val options = persistentListOf(
        DownloadAction.NEXT_1_ITEM to pluralStringResource(downloadAmount, 1, 1),
        DownloadAction.NEXT_5_ITEMS to pluralStringResource(downloadAmount, 5, 5),
        DownloadAction.NEXT_10_ITEMS to pluralStringResource(downloadAmount, 10, 10),
        DownloadAction.NEXT_25_ITEMS to pluralStringResource(downloadAmount, 25, 25),
        DownloadAction.UNVIEWED_ITEMS to stringResource(downloadUnviewed),
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        options.map { (downloadAction, string) ->
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
