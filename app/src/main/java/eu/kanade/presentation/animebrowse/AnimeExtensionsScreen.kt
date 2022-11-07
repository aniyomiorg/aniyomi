package eu.kanade.presentation.animebrowse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import eu.kanade.presentation.animebrowse.components.AnimeExtensionIcon
import eu.kanade.presentation.browse.ExtensionHeader
import eu.kanade.presentation.browse.ExtensionTrustDialog
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.SwipeRefresh
import eu.kanade.presentation.manga.components.DotSeparatorNoSpaceText
import eu.kanade.presentation.util.horizontalPadding
import eu.kanade.presentation.util.plus
import eu.kanade.presentation.util.secondaryItemAlpha
import eu.kanade.presentation.util.topPaddingValues
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animeextension.model.AnimeExtension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.animeextension.AnimeExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.animeextension.AnimeExtensionsPresenter
import eu.kanade.tachiyomi.util.system.LocaleHelper

@Composable
fun AnimeExtensionScreen(
    presenter: AnimeExtensionsPresenter,
    contentPadding: PaddingValues,
    onLongClickItem: (AnimeExtension) -> Unit,
    onClickItemCancel: (AnimeExtension) -> Unit,
    onInstallExtension: (AnimeExtension.Available) -> Unit,
    onUninstallExtension: (AnimeExtension) -> Unit,
    onUpdateExtension: (AnimeExtension.Installed) -> Unit,
    onTrustExtension: (AnimeExtension.Untrusted) -> Unit,
    onOpenExtension: (AnimeExtension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    SwipeRefresh(
        refreshing = presenter.isRefreshing,
        onRefresh = onRefresh,
        enabled = !presenter.isLoading,
    ) {
        when {
            presenter.isLoading -> LoadingScreen()
            presenter.isEmpty -> EmptyScreen(
                textResource = R.string.empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                AnimeExtensionContent(
                    state = presenter,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onInstallExtension = onInstallExtension,
                    onUninstallExtension = onUninstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onTrustExtension = onTrustExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                )
            }
        }
    }
}

@Composable
private fun AnimeExtensionContent(
    state: AnimeExtensionsState,
    contentPadding: PaddingValues,
    onLongClickItem: (AnimeExtension) -> Unit,
    onClickItemCancel: (AnimeExtension) -> Unit,
    onInstallExtension: (AnimeExtension.Available) -> Unit,
    onUninstallExtension: (AnimeExtension) -> Unit,
    onUpdateExtension: (AnimeExtension.Installed) -> Unit,
    onTrustExtension: (AnimeExtension.Untrusted) -> Unit,
    onOpenExtension: (AnimeExtension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
) {
    var trustState by remember { mutableStateOf<AnimeExtension.Untrusted?>(null) }

    FastScrollLazyColumn(
        contentPadding = contentPadding + topPaddingValues,
    ) {
        items(
            items = state.items,
            contentType = {
                when (it) {
                    is AnimeExtensionUiModel.Header -> "header"
                    is AnimeExtensionUiModel.Item -> "item"
                }
            },
            key = {
                when (it) {
                    is AnimeExtensionUiModel.Header -> "animeextensionHeader-${it.hashCode()}"
                    is AnimeExtensionUiModel.Item -> "animeextension-${it.hashCode()}"
                }
            },
        ) { item ->
            when (item) {
                is AnimeExtensionUiModel.Header.Resource -> {
                    val action: @Composable RowScope.() -> Unit =
                        if (item.textRes == R.string.ext_updates_pending) {
                            {
                                Button(onClick = { onClickUpdateAll() }) {
                                    Text(
                                        text = stringResource(R.string.ext_update_all),
                                        style = LocalTextStyle.current.copy(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    )
                                }
                            }
                        } else {
                            {}
                        }
                    ExtensionHeader(
                        textRes = item.textRes,
                        modifier = Modifier.animateItemPlacement(),
                        action = action,
                    )
                }
                is AnimeExtensionUiModel.Header.Text -> {
                    ExtensionHeader(
                        text = item.text,
                        modifier = Modifier.animateItemPlacement(),
                    )
                }
                is AnimeExtensionUiModel.Item -> {
                    AnimeExtensionItem(
                        modifier = Modifier.animateItemPlacement(),
                        item = item,
                        onClickItem = {
                            when (it) {
                                is AnimeExtension.Available -> onInstallExtension(it)
                                is AnimeExtension.Installed -> onOpenExtension(it)
                                is AnimeExtension.Untrusted -> { trustState = it }
                            }
                        },
                        onLongClickItem = onLongClickItem,
                        onClickItemCancel = onClickItemCancel,
                        onClickItemAction = {
                            when (it) {
                                is AnimeExtension.Available -> onInstallExtension(it)
                                is AnimeExtension.Installed -> {
                                    if (it.hasUpdate) {
                                        onUpdateExtension(it)
                                    } else {
                                        onOpenExtension(it)
                                    }
                                }
                                is AnimeExtension.Untrusted -> { trustState = it }
                            }
                        },
                    )
                }
            }
        }
    }
    if (trustState != null) {
        ExtensionTrustDialog(
            onClickConfirm = {
                onTrustExtension(trustState!!)
                trustState = null
            },
            onClickDismiss = {
                onUninstallExtension(trustState!!)
                trustState = null
            },
            onDismissRequest = {
                trustState = null
            },
        )
    }
}

@Composable
private fun AnimeExtensionItem(
    modifier: Modifier = Modifier,
    item: AnimeExtensionUiModel.Item,
    onClickItem: (AnimeExtension) -> Unit,
    onLongClickItem: (AnimeExtension) -> Unit,
    onClickItemCancel: (AnimeExtension) -> Unit,
    onClickItemAction: (AnimeExtension) -> Unit,
) {
    val (extension, installStep) = item
    BaseBrowseItem(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val idle = installStep.isCompleted()
                if (!idle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.dp,
                    )
                }

                val padding by animateDpAsState(targetValue = if (idle) 0.dp else 8.dp)
                AnimeExtensionIcon(
                    extension = extension,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(padding),
                )
            }
        },
        action = {
            AnimeExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemAction = onClickItemAction,
            )
        },
    ) {
        AnimeExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AnimeExtensionItemContent(
    extension: AnimeExtension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = horizontalPadding),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        // Won't look good but it's not like we can ellipsize overflowing content
        FlowRow(
            modifier = Modifier.secondaryItemAlpha(),
            mainAxisSpacing = 4.dp,
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                if (extension is AnimeExtension.Installed && extension.lang.isNotEmpty()) {
                    Text(
                        text = LocaleHelper.getSourceDisplayName(extension.lang, LocalContext.current),
                    )
                }

                if (extension.versionName.isNotEmpty()) {
                    Text(
                        text = extension.versionName,
                    )
                }

                val warning = when {
                    extension is AnimeExtension.Untrusted -> R.string.ext_untrusted
                    extension is AnimeExtension.Installed && extension.isUnofficial -> R.string.ext_unofficial
                    extension is AnimeExtension.Installed && extension.isObsolete -> R.string.ext_obsolete
                    extension.isNsfw -> R.string.ext_nsfw_short
                    else -> null
                }
                if (warning != null) {
                    Text(
                        text = stringResource(warning).uppercase(),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!installStep.isCompleted()) {
                    DotSeparatorNoSpaceText()
                    Text(
                        text = when (installStep) {
                            InstallStep.Pending -> stringResource(R.string.ext_pending)
                            InstallStep.Downloading -> stringResource(R.string.ext_downloading)
                            InstallStep.Installing -> stringResource(R.string.ext_installing)
                            else -> error("Must not show non-install process text")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeExtensionItemActions(
    extension: AnimeExtension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (AnimeExtension) -> Unit = {},
    onClickItemAction: (AnimeExtension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()
    Row(modifier = modifier) {
        if (isIdle) {
            TextButton(
                onClick = { onClickItemAction(extension) },
            ) {
                Text(
                    text = when (installStep) {
                        InstallStep.Installed -> stringResource(R.string.ext_installed)
                        InstallStep.Error -> stringResource(R.string.action_retry)
                        InstallStep.Idle -> {
                            when (extension) {
                                is AnimeExtension.Installed -> {
                                    if (extension.hasUpdate) {
                                        stringResource(R.string.ext_update)
                                    } else {
                                        stringResource(R.string.action_settings)
                                    }
                                }
                                is AnimeExtension.Untrusted -> stringResource(R.string.ext_trust)
                                is AnimeExtension.Available -> stringResource(R.string.ext_install)
                            }
                        }
                        else -> error("Must not show install process text")
                    },
                )
            }
        } else {
            IconButton(onClick = { onClickItemCancel(extension) }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                )
            }
        }
    }
}
