package eu.kanade.presentation.updates.manga

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entries.manga.components.ChapterDownloadAction
import eu.kanade.presentation.updates.manga.components.MangaUpdatesUiChapter
import eu.kanade.presentation.updates.manga.components.MangaUpdatesUiManga
import eu.kanade.presentation.updates.manga.model.MangaUpdatesUiModels
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.updates.manga.MangaUpdatesItem
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate

internal fun LazyListScope.mangaUpdatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "mangaUpdates-lastUpdated") {
        Box(
            modifier = Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        ) {
            Text(
                text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

internal fun LazyListScope.mangaUpdatesUiItems(
    uiModels: List<MangaUpdatesUiModel>,
    selectionMode: Boolean,
    openedMangas: Set<Pair<Long, LocalDate>>,
    onToggleManga: (Long, LocalDate) -> Unit,
    onUpdateSelected: (MangaUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onClickCover: (MangaUpdatesItem) -> Unit,
    onClickUpdate: (MangaUpdatesItem) -> Unit,
    onDownloadChapter: (List<MangaUpdatesItem>, ChapterDownloadAction) -> Unit,
) {
    val rows = uiModels.toRows(openedMangas)

    items(
        items = rows,
        key = {
            when (it) {
                is MangaUpdatesRow.Header -> "mangaUpdatesHeader-${it.date}"
                is MangaUpdatesRow.Manga -> "mangaUpdatesManga-${it.date}-${it.manga.mangaId}"
                is MangaUpdatesRow.Chapter -> {
                    "mangaUpdatesChapter-${it.date}-${it.item.update.mangaId}-${it.item.update.chapterId}"
                }
            }
        },
        contentType = {
            when (it) {
                is MangaUpdatesRow.Header -> "header"
                is MangaUpdatesRow.Manga -> "manga"
                is MangaUpdatesRow.Chapter -> "chapter"
            }
        },
    ) { row ->
        when (row) {
            is MangaUpdatesRow.Header -> {
                ListGroupHeader(
                    modifier = Modifier.animateItemFastScroll(),
                    text = relativeDateText(row.date),
                )
            }
            is MangaUpdatesRow.Manga -> {
                val chapterAmount = row.chapters.size
                val subText = pluralStringResource(
                    AYMR.plurals.updated_amount_chapters,
                    chapterAmount,
                    chapterAmount,
                )
                MangaUpdatesUiManga(
                    modifier = Modifier.animateItemFastScroll(),
                    manga = row.manga.copy(subText = subText),
                    selected = row.chapters.isNotEmpty() && row.chapters.fastAll { it.selected },
                    onClick = { onToggleManga(row.manga.mangaId, row.date) },
                    onLongClick = {
                        row.chapters.forEachIndexed { index, chapter ->
                            onUpdateSelected(chapter, true, true, !selectionMode && index == 0)
                        }
                    },
                    onClickCover = { onClickCover(row.chapters.first()) }.takeIf { !selectionMode },
                    openManga = row.open,
                )
            }
            is MangaUpdatesRow.Chapter -> {
                val chapter = row.item
                val indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                MangaUpdatesUiChapter(
                    modifier = Modifier
                        .animateItemFastScroll()
                        .padding(start = MaterialTheme.padding.large)
                        .drawBehind {
                            drawLine(
                                color = indicatorColor,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 1.dp.toPx(),
                            )
                        },
                    update = chapter.update,
                    selected = chapter.selected,
                    readProgress = chapter.update.lastPageRead
                        .takeIf { !chapter.update.read && it > 0L }
                        ?.let { stringResource(MR.strings.chapter_progress, it + 1) },
                    onLongClick = { onUpdateSelected(chapter, !chapter.selected, true, true) },
                    onClick = {
                        when {
                            selectionMode -> onUpdateSelected(chapter, !chapter.selected, true, false)
                            else -> onClickUpdate(chapter)
                        }
                    },
                    onDownloadChapter = { action: ChapterDownloadAction ->
                        onDownloadChapter(listOf(chapter), action)
                    }.takeIf { !selectionMode },
                    downloadStateProvider = chapter.downloadStateProvider,
                    downloadProgressProvider = chapter.downloadProgressProvider,
                )
            }
        }
    }
}

private fun List<MangaUpdatesUiModel>.toRows(
    openedMangas: Set<Pair<Long, LocalDate>>,
): List<MangaUpdatesRow> {
    val groups = mutableListOf<Pair<LocalDate, MutableList<MangaUpdatesItem>>>()

    for (model in this) {
        when (model) {
            is MangaUpdatesUiModel.Header -> groups.add(model.date to mutableListOf())
            is MangaUpdatesUiModel.Item -> groups.lastOrNull()?.second?.add(model.item)
        }
    }

    return buildList {
        for ((date, items) in groups) {
            add(MangaUpdatesRow.Header(date))
            val groupedItems = items.groupBy { it.update.mangaId }

            for ((mangaId, chapters) in groupedItems) {
                val firstChapter = chapters.firstOrNull() ?: continue
                val manga = MangaUpdatesUiModels.Manga(
                    mangaId = mangaId,
                    mangaTitle = firstChapter.update.mangaTitle,
                    coverData = firstChapter.update.coverData,
                    subText = "",
                )
                val open = mangaId to date in openedMangas

                add(
                    MangaUpdatesRow.Manga(
                        date = date,
                        manga = manga,
                        chapters = chapters,
                        open = open,
                    ),
                )

                if (open) {
                    for (chapter in chapters) {
                        add(
                            MangaUpdatesRow.Chapter(
                                date = date,
                                item = chapter,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private sealed interface MangaUpdatesRow {
    data class Header(val date: LocalDate) : MangaUpdatesRow
    data class Manga(
        val date: LocalDate,
        val manga: MangaUpdatesUiModels.Manga,
        val chapters: List<MangaUpdatesItem>,
        val open: Boolean,
    ) : MangaUpdatesRow

    data class Chapter(
        val date: LocalDate,
        val item: MangaUpdatesItem,
    ) : MangaUpdatesRow
}
