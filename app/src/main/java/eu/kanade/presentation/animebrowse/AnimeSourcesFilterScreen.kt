package eu.kanade.presentation.animebrowse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.animebrowse.components.BaseAnimeSourceItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeFilterUiModel
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourcesFilterPresenter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AnimeSourcesFilterScreen(
    navigateUp: () -> Unit,
    presenter: AnimeSourcesFilterPresenter,
    onClickLang: (String) -> Unit,
    onClickSource: (AnimeSource) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(R.string.label_sources),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        when {
            presenter.isLoading -> LoadingScreen()
            presenter.isEmpty -> EmptyScreen(
                textResource = R.string.source_filter_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                AnimeSourcesFilterContent(
                    contentPadding = contentPadding,
                    state = presenter,
                    onClickLang = onClickLang,
                    onClickSource = onClickSource,
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        presenter.events.collectLatest { event ->
            when (event) {
                AnimeSourcesFilterPresenter.Event.FailedFetchingLanguages -> {
                    context.toast(R.string.internal_error)
                }
            }
        }
    }
}

@Composable
private fun AnimeSourcesFilterContent(
    contentPadding: PaddingValues,
    state: AnimeSourcesFilterState,
    onClickLang: (String) -> Unit,
    onClickSource: (AnimeSource) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = state.items,
            contentType = {
                when (it) {
                    is AnimeFilterUiModel.Header -> "header"
                    is AnimeFilterUiModel.Item -> "item"
                }
            },
            key = {
                when (it) {
                    is AnimeFilterUiModel.Header -> it.hashCode()
                    is AnimeFilterUiModel.Item -> "source-filter-${it.source.key()}"
                }
            },
        ) { model ->
            when (model) {
                is AnimeFilterUiModel.Header -> AnimeSourcesFilterHeader(
                    modifier = Modifier.animateItemPlacement(),
                    language = model.language,
                    enabled = model.enabled,
                    onClickItem = onClickLang,
                )
                is AnimeFilterUiModel.Item -> AnimeSourcesFilterItem(
                    modifier = Modifier.animateItemPlacement(),
                    source = model.source,
                    isEnabled = model.isEnabled,
                    onClickItem = onClickSource,
                )
            }
        }
    }
}

@Composable
fun AnimeSourcesFilterHeader(
    modifier: Modifier,
    language: String,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
) {
    SwitchPreferenceWidget(
        modifier = modifier,
        title = LocaleHelper.getSourceDisplayName(language, LocalContext.current),
        checked = enabled,
        onCheckedChanged = { onClickItem(language) },
    )
}

@Composable
private fun AnimeSourcesFilterItem(
    modifier: Modifier,
    source: AnimeSource,
    isEnabled: Boolean,
    onClickItem: (AnimeSource) -> Unit,
) {
    BaseAnimeSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = false,
        onClickItem = { onClickItem(source) },
        action = {
            Checkbox(checked = isEnabled, onCheckedChange = null)
        },
    )
}
