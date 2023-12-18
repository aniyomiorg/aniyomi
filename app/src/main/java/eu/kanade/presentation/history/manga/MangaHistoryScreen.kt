package eu.kanade.presentation.history.manga

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.RelativeDateHeader
import eu.kanade.presentation.history.manga.components.MangaHistoryItem
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.ui.history.manga.MangaHistoryScreenModel
import tachiyomi.core.preference.InMemoryPreferenceStore
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

@Composable
fun MangaHistoryScreen(
    state: MangaHistoryScreenModel.State,
    snackbarHostState: SnackbarHostState,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onDialogChange: (MangaHistoryScreenModel.Dialog?) -> Unit,
    preferences: UiPreferences = Injekt.get(),
    searchQuery: String? = null,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_manga
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                MangaHistoryContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.mangaId) },
                    onClickResume = { history -> onClickResume(history.mangaId, history.chapterId) },
                    onClickDelete = { item ->
                        onDialogChange(
                            MangaHistoryScreenModel.Dialog.Delete(item),
                        )
                    },
                    preferences = preferences,
                )
            }
        }
    }
}

@Composable
private fun MangaHistoryContent(
    history: List<MangaHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (MangaHistoryWithRelations) -> Unit,
    onClickResume: (MangaHistoryWithRelations) -> Unit,
    onClickDelete: (MangaHistoryWithRelations) -> Unit,
    preferences: UiPreferences,
) {
    val relativeTime = remember { preferences.relativeTime().get() }
    val dateFormat = remember { UiPreferences.dateFormat(preferences.dateFormat().get()) }

    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is MangaHistoryUiModel.Header -> "header"
                    is MangaHistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is MangaHistoryUiModel.Header -> {
                    RelativeDateHeader(
                        modifier = Modifier.animateItemPlacement(),
                        date = item.date,
                        relativeTime = relativeTime,
                        dateFormat = dateFormat,
                    )
                }
                is MangaHistoryUiModel.Item -> {
                    val value = item.item
                    MangaHistoryItem(
                        modifier = Modifier.animateItemPlacement(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                    )
                }
            }
        }
    }
}

sealed interface MangaHistoryUiModel {
    data class Header(val date: Date) : MangaHistoryUiModel
    data class Item(val item: MangaHistoryWithRelations) : MangaHistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(MangaHistoryScreenModelStateProvider::class)
    historyState: MangaHistoryScreenModel.State,
) {
    TachiyomiTheme {
        MangaHistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            searchQuery = null,
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            preferences = UiPreferences(
                InMemoryPreferenceStore(
                    sequenceOf(
                        InMemoryPreferenceStore.InMemoryPreference(
                            key = "relative_time_v2",
                            data = false,
                            defaultValue = false,
                        ),
                    ),
                ),
            ),
        )
    }
}
