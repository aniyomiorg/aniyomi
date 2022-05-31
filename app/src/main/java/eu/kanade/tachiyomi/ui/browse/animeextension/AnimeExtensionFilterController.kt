package eu.kanade.tachiyomi.ui.browse.animeextension

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.presentation.browse.AnimeExtensionFilterScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.ComposeController

class AnimeExtensionFilterController : ComposeController<AnimeExtensionFilterPresenter>() {

    override fun getTitle() = resources?.getString(R.string.label_animeextensions)

    override fun createPresenter(): AnimeExtensionFilterPresenter = AnimeExtensionFilterPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        AnimeExtensionFilterScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickLang = { language ->
                presenter.toggleLanguage(language)
            },
        )
    }
}

data class FilterUiModel(val lang: String, val enabled: Boolean)
