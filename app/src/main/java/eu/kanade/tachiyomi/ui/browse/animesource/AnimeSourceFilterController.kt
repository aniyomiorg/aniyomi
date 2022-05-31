package eu.kanade.tachiyomi.ui.browse.animesource

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.browse.AnimeSourceFilterScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.ComposeController

class AnimeSourceFilterController : ComposeController<AnimeSourceFilterPresenter>() {

    override fun getTitle() = resources?.getString(R.string.label_sources)

    override fun createPresenter(): AnimeSourceFilterPresenter = AnimeSourceFilterPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        AnimeSourceFilterScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickLang = { language ->
                presenter.toggleLanguage(language)
            },
            onClickSource = { source ->
                presenter.toggleSource(source)
            },
        )
    }
}

sealed class AnimeFilterUiModel {
    data class Header(val language: String, val isEnabled: Boolean) : AnimeFilterUiModel()
    data class Item(val source: AnimeSource, val isEnabled: Boolean) : AnimeFilterUiModel()
}
