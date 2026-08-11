package eu.kanade.presentation.more.settings.screen.browse.components.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.domain.extension.anime.model.AnimeExtensionStore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Discord

@Composable
fun AnimeExtensionStoresContent(
    repos: List<AnimeExtensionStore>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onCopy: (AnimeExtensionStore) -> Unit,
    onOpenWebsite: (AnimeExtensionStore) -> Unit,
    onOpenDiscord: (AnimeExtensionStore) -> Unit,
    onClickDelete: (AnimeExtensionStore) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        modifier = modifier,
    ) {
        repos.forEach {
            item {
                AnimeExtensionStoresListItem(
                    modifier = Modifier.animateItem(),
                    store = it,
                    onOpenWebsite = { onOpenWebsite(it) },
                    onOpenDiscord = { onOpenDiscord(it) },
                    onCopy = { onCopy(it) },
                    onDelete = { onClickDelete(it) },
                )
            }
        }
    }
}

@Composable
private fun AnimeExtensionStoresListItem(
    store: AnimeExtensionStore,
    onOpenWebsite: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.padding.medium,
                    top = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = null)
            Text(
                text = store.name,
                modifier = Modifier.padding(start = MaterialTheme.padding.medium),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpenWebsite) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = stringResource(MR.strings.action_open_in_browser),
                )
            }

            if (store.contact.discord != null) {
                IconButton(onClick = onOpenDiscord) {
                    Icon(
                        imageVector = CustomIcons.Discord,
                        contentDescription = null,
                    )
                }
            }

            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}
