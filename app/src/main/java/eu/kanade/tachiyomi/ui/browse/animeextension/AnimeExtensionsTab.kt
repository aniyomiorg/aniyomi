package eu.kanade.tachiyomi.ui.browse.animeextension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.animebrowse.AnimeExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animeextension.model.AnimeExtension
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.animeextension.details.AnimeExtensionDetailsController

@Composable
fun animeExtensionsTab(
    router: Router?,
    presenter: AnimeExtensionsPresenter,
) = TabContent(
    titleRes = R.string.label_animeextensions,
    badgeNumber = presenter.updates.takeIf { it > 0 },
    searchEnabled = true,
    actions = listOf(
        AppBar.Action(
            title = stringResource(R.string.action_filter),
            icon = Icons.Outlined.Translate,
            onClick = { router?.pushController(AnimeExtensionFilterController()) },
        ),
    ),
    content = { contentPadding ->
        AnimeExtensionScreen(
            presenter = presenter,
            contentPadding = contentPadding,
            onLongClickItem = { extension ->
                when (extension) {
                    is AnimeExtension.Available -> presenter.installExtension(extension)
                    else -> presenter.uninstallExtension(extension.pkgName)
                }
            },
            onClickItemCancel = { extension ->
                presenter.cancelInstallUpdateExtension(extension)
            },
            onClickUpdateAll = {
                presenter.updateAllExtensions()
            },
            onInstallExtension = {
                presenter.installExtension(it)
            },
            onOpenExtension = {
                router?.pushController(AnimeExtensionDetailsController(it.pkgName))
            },
            onTrustExtension = {
                presenter.trustSignature(it.signatureHash)
            },
            onUninstallExtension = {
                presenter.uninstallExtension(it.pkgName)
            },
            onUpdateExtension = {
                presenter.updateExtension(it)
            },
            onRefresh = {
                presenter.findAvailableExtensions()
            },
        )
    },
)
