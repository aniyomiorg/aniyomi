package eu.kanade.tachiyomi.ui.browse.animeextension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.animebrowse.AnimeExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animeextension.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.animeextension.details.AnimeExtensionDetailsScreen

@Composable
fun animeExtensionsTab(
    extensionsScreenModel: AnimeExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by extensionsScreenModel.state.collectAsState()
    val searchQuery by extensionsScreenModel.query.collectAsState()
    return TabContent(
        titleRes = R.string.label_animeextensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(R.string.action_filter),
                icon = Icons.Outlined.Translate,
                onClick = { navigator.push(AnimeExtensionFilterScreen()) },
            ),
        ),
        content = { contentPadding, _ ->
            AnimeExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = searchQuery,
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
