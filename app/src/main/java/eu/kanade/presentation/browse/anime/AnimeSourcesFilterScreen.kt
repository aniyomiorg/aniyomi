package eu.kanade.presentation.browse.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.anime.components.BaseAnimeSourceItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.ui.browse.anime.source.AnimeSourcesFilterScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun AnimeSourcesFilterScreen(
    navigateUp: () -> Unit,
    state: AnimeSourcesFilterScreenModel.State.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (AnimeSource) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_sources),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.source_filter_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }
        AnimeSourcesFilterContent(
            contentPadding = contentPadding,
            state = state,
            onClickLanguage = onClickLanguage,
            onClickSource = onClickSource,
        )
    }
}

@Composable
private fun AnimeSourcesFilterContent(
    contentPadding: PaddingValues,
    state: AnimeSourcesFilterScreenModel.State.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (AnimeSource) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        state.items.forEach { (language, sources) ->
            val enabled = language in state.enabledLanguages
            item(
                key = language,
                contentType = "source-filter-header",
            ) {
                AnimeSourcesFilterHeader(
                    modifier = Modifier.animateItem(),
                    language = language,
                    enabled = enabled,
                    onClickItem = onClickLanguage,
                )
            }
            if (enabled) {
                items(
                    items = sources,
                    key = { "source-filter-${it.key()}" },
                    contentType = { "source-filter-item" },
                ) { source ->
                    AnimeSourcesFilterItem(
                        modifier = Modifier.animateItem(),
                        source = source,
                        isEnabled = "${source.id}" !in state.disabledSources,
                        onClickItem = onClickSource,
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeSourcesFilterHeader(
    language: String,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
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
    source: AnimeSource,
    isEnabled: Boolean,
    onClickItem: (AnimeSource) -> Unit,
    modifier: Modifier = Modifier,
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
