package eu.kanade.tachiyomi.ui.browse.manga.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.manga.MangaExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.ui.browse.manga.extension.details.MangaExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun mangaExtensionsTab(
    extensionsScreenModel: MangaExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by extensionsScreenModel.state.collectAsState()

    return TabContent(
        titleRes = MR.strings.label_manga_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = Icons.Outlined.Translate,
                onClick = { navigator.push(MangaExtensionFilterScreen()) },
            ),
        ),
        content = { contentPadding, _ ->
            MangaExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension ->
                    when (extension) {
                        is MangaExtension.Available -> extensionsScreenModel.installExtension(
                            extension,
                        )
                        else -> extensionsScreenModel.uninstallExtension(extension)
                    }
                },
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onClickItemWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { navigator.push(MangaExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustSignature(it.signatureHash) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
            )
        },
    )
}
