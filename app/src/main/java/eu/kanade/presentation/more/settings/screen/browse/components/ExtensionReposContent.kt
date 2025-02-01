package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import mihon.domain.extensionrepo.anime.interactor.CreateAnimeExtensionRepo.Companion.ANIMETAIL_SIGNATURE
import mihon.domain.extensionrepo.anime.interactor.CreateAnimeExtensionRepo.Companion.KEIYOUSHI_SIGNATURE
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
    modifier: Modifier = Modifier,
    // KMK -->
    isDisabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    // KMK <--
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier,
    ) {
        // KMK -->
        Row(
            modifier = Modifier
                .padding(start = MaterialTheme.padding.medium),
        ) {
            val resId = repoResId(repo.signingKeyFingerprint)
            Image(
                bitmap = ImageBitmap.imageResource(id = resId),
                contentDescription = null,
                alpha = if (isDisabled) 0.4f else 1f,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .align(Alignment.CenterVertically),
            )
            Column {
                // KMK <--
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
                    Text(
                        text = repo.name,
                        // KMK: modifier = Modifier.padding(start = MaterialTheme.padding.medium),
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
    }
}

// KMK -->
fun repoResId(baseUrl: String) = when (baseUrl) {
    ANIMETAIL_SIGNATURE -> R.mipmap.animetail
    KEIYOUSHI_SIGNATURE-> R.mipmap.keiyoushi
    else -> R.mipmap.extension
}

@Preview
@Composable
fun ExtensionReposContentPreview() {
    val repos = persistentSetOf(
        ExtensionRepo("https://repo", "Animetail", "", "", ANIMETAIL_SIGNATURE),
        ExtensionRepo("https://repo", "Keiyoushi", "", "", KEIYOUSHI_SIGNATURE),
        ExtensionRepo("https://repo", "Other", "", "", "key2"),
    )
    ExtensionReposContent(
        repos = repos,
        lazyListState = LazyListState(),
        paddingValues = PaddingValues(),
        onOpenWebsite = {},
        onClickDelete = {},
        onClickEnable = {},
        onClickDisable = {},
        disabledRepos = setOf("https://repo"),
    )
}
// KMK <--
