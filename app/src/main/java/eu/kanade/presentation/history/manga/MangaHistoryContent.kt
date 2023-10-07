package eu.kanade.presentation.history.manga

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.RelativeDateHeader
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat

@Composable
fun MangaHistoryContent(
    history: List<MangaHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (MangaHistoryWithRelations) -> Unit,
    onClickResume: (MangaHistoryWithRelations) -> Unit,
    onClickDelete: (MangaHistoryWithRelations) -> Unit,
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
