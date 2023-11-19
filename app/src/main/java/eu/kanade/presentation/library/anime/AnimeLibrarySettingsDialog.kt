package eu.kanade.presentation.library.anime

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEach
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibrarySettingsScreenModel
import tachiyomi.core.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.anime.model.AnimeLibraryGroup
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.anime.model.sort
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.IconItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun AnimeLibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    screenModel: AnimeLibrarySettingsScreenModel,
    category: Category?,
    // SY -->
    hasCategories: Boolean,
    // SY <--
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(R.string.action_filter),
            stringResource(R.string.action_sort),
            stringResource(R.string.action_display),
            // SY -->
            stringResource(R.string.group),
            // SY <--
        ),
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> FilterPage(
                    screenModel = screenModel,
                )
                1 -> SortPage(
                    category = category,
                    screenModel = screenModel,
                )
                2 -> DisplayPage(
                    screenModel = screenModel,
                )
                // SY -->
                3 -> GroupPage(
                    screenModel = screenModel,
                    hasCategories = hasCategories,
                )
                // SY <--
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    screenModel: AnimeLibrarySettingsScreenModel,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloadedAnime().collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_downloaded),
        state = if (downloadedOnly) {
            TriState.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterDownloadedAnime) },
    )
    val filterUnseen by screenModel.libraryPreferences.filterUnseen().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_unseen),
        state = filterUnseen,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterUnseen) },
    )
    val filterStarted by screenModel.libraryPreferences.filterStartedAnime().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_started),
        state = filterStarted,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterStartedAnime) },
    )
    val filterBookmarked by screenModel.libraryPreferences.filterBookmarkedAnime().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_bookmarked),
        state = filterBookmarked,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterBookmarkedAnime) },
    )
    val filterCompleted by screenModel.libraryPreferences.filterCompletedAnime().collectAsState()
    TriStateItem(
        label = stringResource(R.string.completed),
        state = filterCompleted,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterCompletedAnime) },
    )

    val trackServices = remember { screenModel.trackServices }
    when (trackServices.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackServices[0]
            val filterTracker by screenModel.libraryPreferences.filterTrackedAnime(service.id.toInt()).collectAsState()
            TriStateItem(
                label = stringResource(R.string.action_filter_tracked),
                state = filterTracker,
                onClick = { screenModel.toggleTracker(service.id.toInt()) },
            )
        }
        else -> {
            HeadingItem(R.string.action_filter_tracked)
            trackServices.map { service ->
                val filterTracker by screenModel.libraryPreferences.filterTrackedAnime(service.id.toInt()).collectAsState()
                TriStateItem(
                    label = stringResource(service.nameRes()),
                    state = filterTracker,
                    onClick = { screenModel.toggleTracker(service.id.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SortPage(
    category: Category?,
    screenModel: AnimeLibrarySettingsScreenModel,
) {
    // SY -->
    val globalSortMode by screenModel.libraryPreferences.libraryAnimeSortingMode().collectAsState()
    val sortingMode = if (screenModel.grouping == AnimeLibraryGroup.BY_DEFAULT) {
        category.sort.type
    } else {
        globalSortMode.type
    }
    val sortDescending = if (screenModel.grouping == AnimeLibraryGroup.BY_DEFAULT) {
        category.sort.isAscending
    } else {
        globalSortMode.isAscending
    }.not()
    // SY <--

    listOf(
        R.string.action_sort_alpha to AnimeLibrarySort.Type.Alphabetical,
        R.string.action_sort_total_episodes to AnimeLibrarySort.Type.TotalEpisodes,
        R.string.action_sort_last_seen to AnimeLibrarySort.Type.LastSeen,
        R.string.action_sort_last_anime_update to AnimeLibrarySort.Type.LastUpdate,
        R.string.action_sort_unseen_count to AnimeLibrarySort.Type.UnseenCount,
        R.string.action_sort_latest_episode to AnimeLibrarySort.Type.LatestEpisode,
        R.string.action_sort_episode_fetch_date to AnimeLibrarySort.Type.EpisodeFetchDate,
        R.string.action_sort_date_added to AnimeLibrarySort.Type.DateAdded,
        R.string.action_sort_airing_time to AnimeLibrarySort.Type.AiringTime,
    ).map { (titleRes, mode) ->
        SortItem(
            label = stringResource(titleRes),
            sortDescending = sortDescending.takeIf { sortingMode == mode },
            onClick = {
                val isTogglingDirection = sortingMode == mode
                val direction = when {
                    isTogglingDirection -> if (sortDescending) AnimeLibrarySort.Direction.Ascending else AnimeLibrarySort.Direction.Descending
                    else -> if (sortDescending) AnimeLibrarySort.Direction.Descending else AnimeLibrarySort.Direction.Ascending
                }
                screenModel.setSort(category, mode, direction)
            },
        )
    }
}

private val displayModes = listOf(
    R.string.action_display_grid to LibraryDisplayMode.CompactGrid,
    R.string.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
    R.string.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
    R.string.action_display_list to LibraryDisplayMode.List,
)

@Composable
private fun ColumnScope.DisplayPage(
    screenModel: AnimeLibrarySettingsScreenModel,
) {
    val displayMode by screenModel.libraryPreferences.displayMode().collectAsState()
    SettingsChipRow(R.string.action_display_mode) {
        displayModes.map { (titleRes, mode) ->
            FilterChip(
                selected = displayMode == mode,
                onClick = { screenModel.setDisplayMode(mode) },
                label = { Text(stringResource(titleRes)) },
            )
        }
    }

    if (displayMode != LibraryDisplayMode.List) {
        val configuration = LocalConfiguration.current
        val columnPreference = remember {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                screenModel.libraryPreferences.animeLandscapeColumns()
            } else {
                screenModel.libraryPreferences.animePortraitColumns()
            }
        }

        val columns by columnPreference.collectAsState()
        SliderItem(
            label = stringResource(R.string.pref_library_columns),
            max = 10,
            value = columns,
            valueText = if (columns > 0) {
                stringResource(R.string.pref_library_columns_per_row, columns)
            } else {
                stringResource(R.string.label_default)
            },
            onChange = columnPreference::set,
        )
    }

    HeadingItem(R.string.overlay_header)
    CheckboxItem(
        label = stringResource(R.string.action_display_download_badge_anime),
        pref = screenModel.libraryPreferences.downloadBadge(),
    )
    CheckboxItem(
        label = stringResource(R.string.action_display_local_badge),
        pref = screenModel.libraryPreferences.localBadge(),
    )
    CheckboxItem(
        label = stringResource(R.string.action_display_language_badge),
        pref = screenModel.libraryPreferences.languageBadge(),
    )
    CheckboxItem(
        label = stringResource(R.string.action_display_show_continue_reading_button),
        pref = screenModel.libraryPreferences.showContinueViewingButton(),
    )

    HeadingItem(R.string.tabs_header)
    CheckboxItem(
        label = stringResource(R.string.action_display_show_tabs),
        pref = screenModel.libraryPreferences.categoryTabs(),
    )
    CheckboxItem(
        label = stringResource(R.string.action_display_show_number_of_items),
        pref = screenModel.libraryPreferences.categoryNumberOfItems(),
    )
}

data class GroupMode(
    val int: Int,
    val nameRes: Int,
    val drawableRes: Int,
)

private fun groupTypeDrawableRes(type: Int): Int {
    return when (type) {
        AnimeLibraryGroup.BY_STATUS -> R.drawable.ic_progress_clock_24dp
        AnimeLibraryGroup.BY_TRACK_STATUS -> R.drawable.ic_sync_24dp
        AnimeLibraryGroup.BY_SOURCE -> R.drawable.ic_browse_filled_24dp
        AnimeLibraryGroup.BY_TAG -> R.drawable.ic_tag_24dp
        AnimeLibraryGroup.UNGROUPED -> R.drawable.ic_ungroup_24dp
        else -> R.drawable.ic_label_24dp
    }
}

@Composable
private fun ColumnScope.GroupPage(
    screenModel: AnimeLibrarySettingsScreenModel,
    hasCategories: Boolean,
) {
    val groups = remember(hasCategories, screenModel.trackServices) {
        buildList {
            add(AnimeLibraryGroup.BY_DEFAULT)
            add(AnimeLibraryGroup.BY_SOURCE)
            add(AnimeLibraryGroup.BY_TAG)
            add(AnimeLibraryGroup.BY_STATUS)
            if (screenModel.trackServices.isNotEmpty()) {
                add(AnimeLibraryGroup.BY_TRACK_STATUS)
            }
            if (hasCategories) {
                add(AnimeLibraryGroup.UNGROUPED)
            }
        }.map {
            GroupMode(
                it,
                AnimeLibraryGroup.groupTypeStringRes(it, hasCategories),
                groupTypeDrawableRes(it),
            )
        }
    }

    groups.fastForEach {
        IconItem(
            label = stringResource(it.nameRes),
            icon = painterResource(it.drawableRes),
            selected = it.int == screenModel.grouping,
            onClick = {
                screenModel.setGrouping(it.int)
            },
        )
    }
}
