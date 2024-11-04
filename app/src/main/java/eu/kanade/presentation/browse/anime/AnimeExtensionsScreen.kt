package eu.kanade.presentation.browse.anime

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.BaseBrowseItem
import eu.kanade.presentation.browse.anime.components.AnimeExtensionIcon
import eu.kanade.presentation.browse.manga.ExtensionHeader
import eu.kanade.presentation.browse.manga.ExtensionTrustDialog
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.entries.components.DotSeparatorNoSpaceText
import eu.kanade.presentation.more.settings.screen.browse.AnimeExtensionReposScreen
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun AnimeExtensionScreen(
    state: AnimeExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (AnimeExtension) -> Unit,
    onClickItemCancel: (AnimeExtension) -> Unit,
    onOpenWebView: (AnimeExtension.Available) -> Unit,
    onInstallExtension: (AnimeExtension.Available) -> Unit,
    onUninstallExtension: (AnimeExtension) -> Unit,
    onUpdateExtension: (AnimeExtension.Installed) -> Unit,
    onTrustExtension: (AnimeExtension.Untrusted) -> Unit,
    onOpenExtension: (AnimeExtension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow

    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.isEmpty -> {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.empty_screen
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                    actions = persistentListOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.label_extension_repos,
                            icon = Icons.Outlined.Settings,
                            onClick = { navigator.push(AnimeExtensionReposScreen()) },
                        ),
                    ),
                )
            }
            else -> {
                AnimeExtensionContent(
                    state = state,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onOpenWebView = onOpenWebView,
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
    state: AnimeExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    onLongClickItem: (AnimeExtension) -> Unit,
    onOpenWebView: (AnimeExtension.Available) -> Unit,
    onClickItemCancel: (AnimeExtension) -> Unit,
    onInstallExtension: (AnimeExtension.Available) -> Unit,
    onUninstallExtension: (AnimeExtension) -> Unit,
    onUpdateExtension: (AnimeExtension.Installed) -> Unit,
    onTrustExtension: (AnimeExtension.Untrusted) -> Unit,
    onOpenExtension: (AnimeExtension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
) {
    val context = LocalContext.current
    var trustState by remember { mutableStateOf<AnimeExtension.Untrusted?>(null) }
    val installGranted = rememberRequestPackageInstallsPermissionState(initialValue = true)

    FastScrollLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        if (!installGranted && state.installer?.requiresSystemPermission == true) {
            item(key = "extension-permissions-warning") {
                WarningBanner(
                    textRes = MR.strings.ext_permission_install_apps_warning,
                    modifier = Modifier.clickable {
                        context.launchRequestPackageInstallsPermission()
                    },
                )
            }
        }

        state.items.forEach { (header, items) ->
            item(
                contentType = "header",
                key = "extensionHeader-${header.hashCode()}",
            ) {
                when (header) {
                    is AnimeExtensionUiModel.Header.Resource -> {
                        val action: @Composable RowScope.() -> Unit =
                            if (header.textRes == MR.strings.ext_updates_pending) {
                                {
                                    Button(onClick = { onClickUpdateAll() }) {
                                        Text(
                                            text = stringResource(MR.strings.ext_update_all),
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
                            textRes = header.textRes,
                            modifier = Modifier.animateItemFastScroll(),
                            action = action,
                        )
                    }
                    is AnimeExtensionUiModel.Header.Text -> {
                        ExtensionHeader(
                            text = header.text,
                            modifier = Modifier.animateItemFastScroll(),
                        )
                    }
                }
            }

            items(
                items = items,
                contentType = { "item" },
                key = { item ->
                    when (item.extension) {
                        is AnimeExtension.Untrusted -> "extension-untrusted-${item.hashCode()}"
                        is AnimeExtension.Installed -> "extension-installed-${item.hashCode()}"
                        is AnimeExtension.Available -> "extension-available-${item.hashCode()}"
                    }
                },
            ) { item ->
                AnimeExtensionItem(
                    item = item,
                    modifier = Modifier.animateItemFastScroll(),
                    onClickItem = {
                        when (it) {
                            is AnimeExtension.Available -> onInstallExtension(it)
                            is AnimeExtension.Installed -> onOpenExtension(it)
                            is AnimeExtension.Untrusted -> {
                                trustState = it
                            }
                        }
                    },
                    onLongClickItem = onLongClickItem,
                    onClickItemSecondaryAction = {
                        when (it) {
                            is AnimeExtension.Available -> onOpenWebView(it)
                            is AnimeExtension.Installed -> onOpenExtension(it)
                            else -> {}
                        }
                    },
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

                            is AnimeExtension.Untrusted -> {
                                trustState = it
                            }
                        }
                    },
                )
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
    item: AnimeExtensionUiModel.Item,
    onClickItem: (AnimeExtension) -> Unit,
    onLongClickItem: (AnimeExtension) -> Unit,
    onClickItemCancel: (AnimeExtension) -> Unit,
    onClickItemAction: (AnimeExtension) -> Unit,
    modifier: Modifier = Modifier,
    onClickItemSecondaryAction: (AnimeExtension) -> Unit,
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

                val padding by animateDpAsState(
                    targetValue = if (idle) 0.dp else 8.dp,
                    label = "iconPadding",
                )
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
                onClickItemSecondaryAction = onClickItemSecondaryAction,
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
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
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
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                if (extension is AnimeExtension.Installed && extension.lang.isNotEmpty()) {
                    Text(
                        text = LocaleHelper.getSourceDisplayName(
                            extension.lang,
                            LocalContext.current,
                        ),
                    )
                }

                if (extension.versionName.isNotEmpty()) {
                    Text(
                        text = extension.versionName,
                    )
                }

                val warning = when {
                    extension is AnimeExtension.Untrusted -> MR.strings.ext_untrusted
                    extension is AnimeExtension.Installed && extension.isObsolete -> MR.strings.ext_obsolete
                    extension.isNsfw -> MR.strings.ext_nsfw_short
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
                            InstallStep.Pending -> stringResource(MR.strings.ext_pending)
                            InstallStep.Downloading -> stringResource(MR.strings.ext_downloading)
                            InstallStep.Installing -> stringResource(MR.strings.ext_installing)
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
    onClickItemSecondaryAction: (AnimeExtension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        when {
            !isIdle -> {
                IconButton(onClick = { onClickItemCancel(extension) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.action_cancel),
                    )
                }
            }
            installStep == InstallStep.Error -> {
                IconButton(onClick = { onClickItemAction(extension) }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(MR.strings.action_retry),
                    )
                }
            }
            installStep == InstallStep.Idle -> {
                when (extension) {
                    is AnimeExtension.Installed -> {
                        IconButton(onClick = { onClickItemSecondaryAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(MR.strings.action_settings),
                            )
                        }

                        if (extension.hasUpdate) {
                            IconButton(onClick = { onClickItemAction(extension) }) {
                                Icon(
                                    imageVector = Icons.Outlined.GetApp,
                                    contentDescription = stringResource(MR.strings.ext_update),
                                )
                            }
                        }
                    }
                    is AnimeExtension.Untrusted -> {
                        IconButton(onClick = { onClickItemAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = stringResource(MR.strings.ext_trust),
                            )
                        }
                    }
                    is AnimeExtension.Available -> {
                        if (extension.sources.isNotEmpty()) {
                            IconButton(
                                onClick = { onClickItemSecondaryAction(extension) },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = stringResource(MR.strings.action_open_in_web_view),
                                )
                            }
                        }

                        IconButton(onClick = { onClickItemAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.GetApp,
                                contentDescription = stringResource(MR.strings.ext_install),
                            )
                        }
                    }
                }
            }
        }
    }
}
