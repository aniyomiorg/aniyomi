package eu.kanade.tachiyomi.ui.browse.anime.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.AnimeExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.anime.extension.details.AnimeExtensionDetailsScreen

@Composable
fun animeExtensionsTab(
    extensionsScreenModel: AnimeExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by extensionsScreenModel.state.collectAsState()

    return TabContent(
        titleRes = R.string.label_anime_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(R.string.action_filter),
                icon = Icons.Outlined.Translate,
                onClick = { navigator.push(eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionFilterScreen()) },
            ),
        ),
        content = { contentPadding, _ ->
            AnimeExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension ->
                    when (extension) {
                        is AnimeExtension.Available -> extensionsScreenModel.installExtension(extension)
                        else -> extensionsScreenModel.uninstallExtension(extension.pkgName)
                    }
                },
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { navigator.push(AnimeExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustSignature(it.signatureHash) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it.pkgName) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
            )
        },
    )
}
