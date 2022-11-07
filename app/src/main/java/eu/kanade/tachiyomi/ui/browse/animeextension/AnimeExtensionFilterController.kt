package eu.kanade.tachiyomi.ui.browse.animeextension

import androidx.compose.runtime.Composable
import eu.kanade.presentation.animebrowse.AnimeExtensionFilterScreen
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController

class AnimeExtensionFilterController : FullComposeController<AnimeExtensionFilterPresenter>() {

    override fun createPresenter() = AnimeExtensionFilterPresenter()

    @Composable
    override fun ComposeContent() {
        AnimeExtensionFilterScreen(
            navigateUp = router::popCurrentController,
            presenter = presenter,
        )
    }
}

data class AnimeFilterUiModel(val lang: String, val enabled: Boolean)
