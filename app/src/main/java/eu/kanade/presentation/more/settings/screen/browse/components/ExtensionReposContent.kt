package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import mihon.domain.extensionrepo.model.ExtensionRepo
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionReposContent(
    repos: ImmutableSet<ExtensionRepo>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onOpenWebsite: (ExtensionRepo) -> Unit,
    onClickDelete: (String) -> Unit,
    // KMK -->
    onClickEnable: (String) -> Unit,
    onClickDisable: (String) -> Unit,
    disabledRepos: Set<String>,
    // KMK <--
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
                ExtensionRepoListItem(
                    modifier = Modifier.animateItem(),
                    repo = it,
                    onOpenWebsite = { onOpenWebsite(it) },
                    onDelete = { onClickDelete(it.baseUrl) },
                    // KMK -->
                    onEnable = { onClickEnable(it.baseUrl) },
                    onDisable = { onClickDisable(it.baseUrl) },
                    isDisabled = it.baseUrl in disabledRepos,
                    // KMK <--
                )
            }
        }
    }
}

@Composable
private fun ExtensionRepoListItem(
    repo: ExtensionRepo,
    onOpenWebsite: () -> Unit,
    onDelete: () -> Unit,
    // KMK -->
    isDisabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    // KMK <--
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Label,
                contentDescription = null,
                // KMK -->
                tint = LocalContentColor.current.let { if (isDisabled) it.copy(alpha = 0.6f) else it },
                // KMK <--
            )
            Text(
                text = repo.name,
                modifier = Modifier.padding(start = MaterialTheme.padding.medium),
                style = MaterialTheme.typography.titleMedium,
                // KMK -->
                color = LocalContentColor.current.let { if (isDisabled) it.copy(alpha = 0.6f) else it },
                textDecoration = TextDecoration.LineThrough.takeIf { isDisabled },
                // KMK <--
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpenWebsite) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = stringResource(MR.strings.action_open_in_browser),
                )
            }

            IconButton(
                onClick = {
                    val url = "${repo.baseUrl}/index.min.json"
                    context.copyToClipboard(url, url)
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                )
            }

            // KMK -->
            IconButton(onClick = if (isDisabled) onEnable else onDisable) {
                Icon(
                    imageVector = if (isDisabled) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(MR.strings.action_disable),
                )
            }
            // KMK <--

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}
// KMK -->
@Preview
@Composable
fun ExtensionReposContentPreview() {
    val repos = persistentSetOf(
        ExtensionRepo("url1", "Repo 1", "", "", "key1"),
        ExtensionRepo("url2", "Repo 2", "", "", "key2"),
    )
    ExtensionReposContent(
        repos = repos,
        lazyListState = LazyListState(),
        paddingValues = PaddingValues(),
        onOpenWebsite = {},
        onClickDelete = {},
        onClickEnable = {},
        onClickDisable = {},
        disabledRepos = setOf("url2"),
    )
}
// KMK <--
