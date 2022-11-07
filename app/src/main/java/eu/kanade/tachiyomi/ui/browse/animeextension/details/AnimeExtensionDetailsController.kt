package eu.kanade.tachiyomi.ui.browse.animeextension.details

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import eu.kanade.presentation.animebrowse.AnimeExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController

@SuppressLint("RestrictedApi")
class AnimeExtensionDetailsController(
    bundle: Bundle? = null,
) : FullComposeController<AnimeExtensionDetailsPresenter>(bundle) {

    constructor(pkgName: String) : this(
        bundleOf(PKGNAME_KEY to pkgName),
    )

    override fun createPresenter() = AnimeExtensionDetailsPresenter(args.getString(PKGNAME_KEY)!!)

    @Composable
    override fun ComposeContent() {
        AnimeExtensionDetailsScreen(
            navigateUp = router::popCurrentController,
            presenter = presenter,
            onClickSourcePreferences = { router.pushController(AnimeSourcePreferencesController(it)) },
        )
    }

    fun onExtensionUninstalled() {
        router.popCurrentController()
    }
}

private const val PKGNAME_KEY = "pkg_name"
