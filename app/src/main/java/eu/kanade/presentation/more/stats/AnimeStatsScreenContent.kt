package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.components.StatsSection
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.util.toDurationString
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.material.padding
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun AnimeStatsScreenContent(
    state: StatsScreenState.SuccessAnime,
    paddingValues: PaddingValues,
) {
    val statListState = rememberLazyListState()
    LazyColumn(
        state = statListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item {
            OverviewSection(state.overview)
        }
        item {
            TitlesStats(state.titles)
        }
        item {
            EpisodeStats(state.episodes)
        }
        item {
            TrackerStats(state.trackers)
        }
    }
}

@Composable
private fun OverviewSection(
    data: StatsData.AnimeOverview,
) {
    val none = stringResource(R.string.none)
    val context = LocalContext.current
    val readDurationString = remember(data.totalSeenDuration) {
        data.totalSeenDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }
    StatsSection(R.string.label_overview_section) {
        Row {
            StatsOverviewItem(
                title = data.libraryAnimeCount.toString(),
                subtitle = stringResource(R.string.in_library),
                icon = Icons.Outlined.CollectionsBookmark,
            )
            StatsOverviewItem(
                title = data.completedAnimeCount.toString(),
                subtitle = stringResource(R.string.label_completed_titles),
                icon = Icons.Outlined.LocalLibrary,
            )
            StatsOverviewItem(
                title = readDurationString,
                subtitle = stringResource(R.string.label_watched_duration),
                icon = Icons.Outlined.Schedule,
            )
        }
    }
}

@Composable
private fun TitlesStats(
    data: StatsData.AnimeTitles,
) {
    StatsSection(R.string.label_titles_section) {
        Row {
            StatsItem(
                data.globalUpdateItemCount.toString(),
                stringResource(R.string.label_titles_in_global_update),
            )
            StatsItem(
                data.startedAnimeCount.toString(),
                stringResource(R.string.label_started),
            )
            StatsItem(
                data.localAnimeCount.toString(),
                stringResource(R.string.label_local),
            )
        }
    }
}

@Composable
private fun EpisodeStats(
    data: StatsData.Episodes,
) {
    StatsSection(R.string.episodes) {
        Row {
            StatsItem(
                data.totalEpisodeCount.toString(),
                stringResource(R.string.label_total_chapters),
            )
            StatsItem(
                data.readEpisodeCount.toString(),
                stringResource(R.string.label_watched_episodes),
            )
            StatsItem(
                data.downloadCount.toString(),
                stringResource(R.string.label_downloaded),
            )
        }
    }
}

@Composable
private fun TrackerStats(
    data: StatsData.Trackers,
) {
    val notApplicable = stringResource(R.string.not_applicable)
    val meanScoreStr = remember(data.trackedTitleCount, data.meanScore) {
        if (data.trackedTitleCount > 0 && !data.meanScore.isNaN()) {
            // All other numbers are localized in English
            String.format(Locale.ENGLISH, "%.2f ★", data.meanScore)
        } else {
            notApplicable
        }
    }
    StatsSection(R.string.label_tracker_section) {
        Row {
            StatsItem(
                data.trackedTitleCount.toString(),
                stringResource(R.string.label_tracked_titles),
            )
            StatsItem(
                meanScoreStr,
                stringResource(R.string.label_mean_score),
            )
            StatsItem(
                data.trackerCount.toString(),
                stringResource(R.string.label_used),
            )
        }
    }
}
