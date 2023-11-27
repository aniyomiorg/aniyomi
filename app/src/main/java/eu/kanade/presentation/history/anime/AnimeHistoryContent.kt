package eu.kanade.presentation.history.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.RelativeDateHeader
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.presentation.core.components.FastScrollLazyColumn

@Composable
fun AnimeHistoryContent(
    history: List<AnimeHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (AnimeHistoryWithRelations) -> Unit,
    onClickResume: (AnimeHistoryWithRelations) -> Unit,
    onClickDelete: (AnimeHistoryWithRelations) -> Unit,
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
                    is AnimeHistoryUiModel.Header -> "header"
                    is AnimeHistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is AnimeHistoryUiModel.Header -> {
                    RelativeDateHeader(

                        date = item.date,
                        relativeTime = relativeTime,
                        dateFormat = dateFormat,
                    )
                }
                is AnimeHistoryUiModel.Item -> {
                    val value = item.item
                    AnimeHistoryItem(

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
