package eu.kanade.tachiyomi.ui.browse.manga.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.manga.MangaExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.ui.browse.manga.extension.details.MangaExtensionDetailsScreen

@Composable
fun mangaExtensionsTab(
    extensionsScreenModel: MangaExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by extensionsScreenModel.state.collectAsState()

    return TabContent(
        titleRes = R.string.label_manga_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(R.string.action_filter),
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
                        is MangaExtension.Available -> extensionsScreenModel.installExtension(extension)
                        else -> extensionsScreenModel.uninstallExtension(extension.pkgName)
                    }
                },
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { navigator.push(MangaExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustSignature(it.signatureHash) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it.pkgName) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
            )
        },
    )
}
