package eu.kanade.presentation.more.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.tachiyomi.util.toSize
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

data class StorageItem(
    val id: Long,
    val title: String,
    val size: Long,
    val thumbnail: String?,
    val entriesCount: Int,
    val color: Color,
)

@Composable
fun StorageItem(
    item: StorageItem,
    isManga: Boolean,
    modifier: Modifier = Modifier,
    onDelete: (Long) -> Unit,
) {
    val pluralCount = if (isManga) MR.plurals.manga_num_chapters else AYMR.plurals.anime_num_episodes
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            ItemCover.Square(
                modifier = Modifier.height(48.dp),
                data = item.thumbnail,
                contentDescription = item.title,
            )
            Column(
                modifier = Modifier.weight(1f),
                content = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.W700,
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Box(
                                modifier = Modifier
                                    .background(item.color, CircleShape)
                                    .size(12.dp),
                            )
                            Spacer(Modifier.width(MaterialTheme.padding.small))
                            Text(
                                text = item.size.toSize(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = MaterialTheme.padding.small / 2)
                                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .size(MaterialTheme.padding.small / 2),
                            )
                            Text(
                                text = pluralStringResource(
                                    pluralCount,
                                    count = item.entriesCount,
                                    item.entriesCount,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                    )
                },
            )
            IconButton(
                onClick = {
                    showDeleteDialog = true
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(MR.strings.action_delete),
                    )
                },
            )
        },
    )

    if (showDeleteDialog) {
        ItemDeleteDialog(
            title = item.title,
            isManga = isManga,
            onDismissRequest = { showDeleteDialog = false },
            onDelete = {
                onDelete(item.id)
            },
        )
    }
}

@Composable
private fun ItemDeleteDialog(
    title: String,
    isManga: Boolean,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismissRequest()
                },
                content = {
                    Text(text = stringResource(MR.strings.action_ok))
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                content = {
                    Text(text = stringResource(MR.strings.action_cancel))
                },
            )
        },
        title = {
            Text(
                text = stringResource(
                    if (isManga) MR.strings.delete_downloads_for_manga else AYMR.strings.delete_downloads_for_anime,
                ),
            )
        },
        text = {
            Text(
                text = stringResource(AYMR.strings.delete_confirmation, title),
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun StorageItemPreview() {
    StorageItem(
        item = StorageItem(
            id = 0L,
            title = "Manga Title",
            size = 123456789L,
            thumbnail = null,
            entriesCount = 123,
            color = Color.Red,
        ),
        isManga = true,
        onDelete = {
        },
    )
}
