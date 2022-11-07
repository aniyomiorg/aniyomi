package eu.kanade.tachiyomi.ui.browse.animesource

import androidx.compose.runtime.Composable
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.animebrowse.AnimeSourcesFilterScreen
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController

class AnimeSourceFilterController : FullComposeController<AnimeSourcesFilterPresenter>() {

    override fun createPresenter(): AnimeSourcesFilterPresenter = AnimeSourcesFilterPresenter()

    @Composable
    override fun ComposeContent() {
        AnimeSourcesFilterScreen(
            navigateUp = router::popCurrentController,
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
    data class Header(val language: String, val enabled: Boolean) : AnimeFilterUiModel()
    data class Item(val source: AnimeSource, val isEnabled: Boolean) : AnimeFilterUiModel()
}
