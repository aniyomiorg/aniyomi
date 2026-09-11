package eu.kanade.presentation.updates.anime

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
import eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.updates.anime.components.AnimeUpdatesUiAnime
import eu.kanade.presentation.updates.anime.components.AnimeUpdatesUiEpisode
import eu.kanade.presentation.updates.anime.model.AnimeUpdatesUiModels
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesItem
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.util.concurrent.TimeUnit

internal fun LazyListScope.animeUpdatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "animeUpdates-lastUpdated") {
        Box(
            modifier = Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .padding(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.small,
                ),
        ) {
            Text(
                text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

internal fun LazyListScope.animeUpdatesUiItems(
    uiModels: List<AnimeUpdatesUiModel>,
    selectionMode: Boolean,
    openedAnimes: Set<Pair<Long, LocalDate>>,
    onToggleAnime: (Long, LocalDate) -> Unit,
    onUpdateSelected: (AnimeUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onClickCover: (AnimeUpdatesItem) -> Unit,
    onClickUpdate: (AnimeUpdatesItem, altPlayer: Boolean) -> Unit,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
) {
    val rows = uiModels.toRows(openedAnimes)

    items(
        items = rows,
        key = {
            when (it) {
                is AnimeUpdatesRow.Header -> "animeUpdatesHeader-${it.date}"
                is AnimeUpdatesRow.Anime -> "animeUpdatesAnime-${it.date}-${it.anime.animeId}"
                is AnimeUpdatesRow.Episode -> {
                    "animeUpdatesEpisode-${it.date}-${it.item.update.animeId}-${it.item.update.episodeId}"
                }
            }
        },
        contentType = {
            when (it) {
                is AnimeUpdatesRow.Header -> "header"
                is AnimeUpdatesRow.Anime -> "anime"
                is AnimeUpdatesRow.Episode -> "episode"
            }
        },
    ) { row ->
        when (row) {
            is AnimeUpdatesRow.Header -> {
                ListGroupHeader(
                    modifier = Modifier.animateItemFastScroll(),
                    text = relativeDateText(row.date),
                )
            }
            is AnimeUpdatesRow.Anime -> {
                val episodeAmount = row.episodes.size
                val subText = pluralStringResource(
                    AYMR.plurals.updated_amount_episodes,
                    episodeAmount,
                    episodeAmount,
                )
                AnimeUpdatesUiAnime(
                    modifier = Modifier.animateItemFastScroll(),
                    anime = row.anime.copy(subText = subText),
                    selected = row.episodes.isNotEmpty() && row.episodes.fastAll { it.selected },
                    onClick = { onToggleAnime(row.anime.animeId, row.date) },
                    onLongClick = {
                        row.episodes.forEachIndexed { index, episode ->
                            onUpdateSelected(episode, true, true, !selectionMode && index == 0)
                        }
                    },
                    onClickCover = { onClickCover(row.episodes.first()) }.takeIf { !selectionMode },
                    openAnime = row.open,
                )
            }
            is AnimeUpdatesRow.Episode -> {
                val episode = row.item
                val indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                AnimeUpdatesUiEpisode(
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
                    update = episode.update,
                    selected = episode.selected,
                    watchProgress = episode.update.lastSecondSeen
                        .takeIf { it > 0 }
                        ?.let {
                            stringResource(
                                AYMR.strings.episode_progress,
                                formatProgress(it),
                                formatProgress(episode.update.totalSeconds),
                            )
                        },
                    onLongClick = { onUpdateSelected(episode, !episode.selected, true, true) },
                    onClick = {
                        when {
                            selectionMode -> onUpdateSelected(episode, !episode.selected, true, false)
                            else -> onClickUpdate(episode, false)
                        }
                    },
                    onClickCover = { onClickCover(episode) }.takeIf { !selectionMode },
                    onDownloadEpisode = { action: EpisodeDownloadAction ->
                        onDownloadEpisode(listOf(episode), action)
                    }.takeIf { !selectionMode },
                    downloadStateProvider = episode.downloadStateProvider,
                    downloadProgressProvider = episode.downloadProgressProvider,
                )
            }
        }
    }
}

private fun List<AnimeUpdatesUiModel>.toRows(
    openedAnimes: Set<Pair<Long, LocalDate>>,
): List<AnimeUpdatesRow> {
    val groups = mutableListOf<Pair<LocalDate, MutableList<AnimeUpdatesItem>>>()

    for (model in this) {
        when (model) {
            is AnimeUpdatesUiModel.Header -> groups.add(model.date to mutableListOf())
            is AnimeUpdatesUiModel.Item -> groups.lastOrNull()?.second?.add(model.item)
        }
    }

    return buildList {
        for ((date, items) in groups) {
            add(AnimeUpdatesRow.Header(date))
            val groupedItems = items.groupBy { it.update.animeId }

            for ((animeId, episodes) in groupedItems) {
                val firstEpisode = episodes.firstOrNull() ?: continue
                val anime = AnimeUpdatesUiModels.Anime(
                    animeId = animeId,
                    animeTitle = firstEpisode.update.animeTitle,
                    coverData = firstEpisode.update.coverData,
                    subText = "",
                )
                val open = animeId to date in openedAnimes

                add(
                    AnimeUpdatesRow.Anime(
                        date = date,
                        anime = anime,
                        episodes = episodes,
                        open = open,
                    ),
                )

                if (open) {
                    for (episode in episodes) {
                        add(
                            AnimeUpdatesRow.Episode(
                                date = date,
                                item = episode,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private sealed interface AnimeUpdatesRow {
    data class Header(val date: LocalDate) : AnimeUpdatesRow
    data class Anime(
        val date: LocalDate,
        val anime: AnimeUpdatesUiModels.Anime,
        val episodes: List<AnimeUpdatesItem>,
        val open: Boolean,
    ) : AnimeUpdatesRow

    data class Episode(
        val date: LocalDate,
        val item: AnimeUpdatesItem,
    ) : AnimeUpdatesRow
}

private fun formatProgress(milliseconds: Long): String {
    return if (milliseconds > 3600000L) {
        String.format(
            "%d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(milliseconds),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}
