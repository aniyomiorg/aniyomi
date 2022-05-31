package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.browse.components.BaseAnimeSourceItem
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.PreferenceRow
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeFilterUiModel
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourceFilterPresenter
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourceFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper

@Composable
fun AnimeSourceFilterScreen(
    nestedScrollInterop: NestedScrollConnection,
    presenter: AnimeSourceFilterPresenter,
    onClickLang: (String) -> Unit,
    onClickSource: (AnimeSource) -> Unit,
) {
    val state by presenter.state.collectAsState()

    when (state) {
        is AnimeSourceFilterState.Loading -> LoadingScreen()
        is AnimeSourceFilterState.Error -> Text(text = (state as AnimeSourceFilterState.Error).error!!.message!!)
        is AnimeSourceFilterState.Success ->
            AnimeSourceFilterContent(
                nestedScrollInterop = nestedScrollInterop,
                items = (state as AnimeSourceFilterState.Success).models,
                onClickLang = onClickLang,
                onClickSource = onClickSource,
            )
    }
}

@Composable
fun AnimeSourceFilterContent(
    nestedScrollInterop: NestedScrollConnection,
    items: List<AnimeFilterUiModel>,
    onClickLang: (String) -> Unit,
    onClickSource: (AnimeSource) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyScreen(textResource = R.string.source_filter_empty_screen)
        return
    }
    LazyColumn(
        modifier = Modifier.nestedScroll(nestedScrollInterop),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        items(
            items = items,
            contentType = {
                when (it) {
                    is AnimeFilterUiModel.Header -> "header"
                    is AnimeFilterUiModel.Item -> "item"
                }
            },
            key = {
                when (it) {
                    is AnimeFilterUiModel.Header -> it.hashCode()
                    is AnimeFilterUiModel.Item -> it.source.key()
                }
            },
        ) { model ->
            when (model) {
                is AnimeFilterUiModel.Header -> {
                    AnimeSourceFilterHeader(
                        modifier = Modifier.animateItemPlacement(),
                        language = model.language,
                        isEnabled = model.isEnabled,
                        onClickItem = onClickLang,
                    )
                }
                is AnimeFilterUiModel.Item -> AnimeSourceFilterItem(
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
fun AnimeSourceFilterHeader(
    modifier: Modifier,
    language: String,
    isEnabled: Boolean,
    onClickItem: (String) -> Unit,
) {
    PreferenceRow(
        modifier = modifier,
        title = LocaleHelper.getSourceDisplayName(language, LocalContext.current),
        action = {
            Switch(checked = isEnabled, onCheckedChange = null)
        },
        onClick = { onClickItem(language) },
    )
}

@Composable
fun AnimeSourceFilterItem(
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
