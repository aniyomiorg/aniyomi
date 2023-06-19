package eu.kanade.presentation.animehistory.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.RelativeDateHeader
import eu.kanade.presentation.history.anime.AnimeHistoryItem
import eu.kanade.presentation.history.anime.AnimeHistoryUiModel
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat

@Composable
fun AnimeHistoryContent(
    history: List<AnimeHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (AnimeHistoryWithRelations) -> Unit,
    onClickResume: (AnimeHistoryWithRelations) -> Unit,
    onClickDelete: (AnimeHistoryWithRelations) -> Unit,
    preferences: UiPreferences = Injekt.get(),
) {
    val relativeTime: Int = remember { preferences.relativeTime().get() }
    val dateFormat: DateFormat = remember { UiPreferences.dateFormat(preferences.dateFormat().get()) }

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
                        modifier = Modifier.animateItemPlacement(),
                        date = item.date,
                        relativeTime = relativeTime,
                        dateFormat = dateFormat,
                    )
                }
                is AnimeHistoryUiModel.Item -> {
                    val value = item.item
                    AnimeHistoryItem(
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
