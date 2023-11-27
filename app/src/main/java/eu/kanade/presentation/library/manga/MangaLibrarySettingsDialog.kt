package eu.kanade.presentation.library.manga

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
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.manga.MangaLibrarySettingsScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.core.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.manga.model.MangaLibrarySort
import tachiyomi.domain.library.manga.model.sort
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun MangaLibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    screenModel: MangaLibrarySettingsScreenModel,
    category: Category?,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = persistentListOf(
            stringResource(R.string.action_filter),
            stringResource(R.string.action_sort),
            stringResource(R.string.action_display),
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
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    screenModel: MangaLibrarySettingsScreenModel,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloadedManga().collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_downloaded),
        state = if (downloadedOnly) {
            TriState.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterDownloadedManga) },
    )
    val filterUnread by screenModel.libraryPreferences.filterUnread().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_unread),
        state = filterUnread,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterUnread) },
    )
    val filterStarted by screenModel.libraryPreferences.filterStartedManga().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_started),
        state = filterStarted,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterStartedManga) },
    )
    val filterBookmarked by screenModel.libraryPreferences.filterBookmarkedManga().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_bookmarked),
        state = filterBookmarked,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterBookmarkedManga) },
    )
    val filterCompleted by screenModel.libraryPreferences.filterCompletedManga().collectAsState()
    TriStateItem(
        label = stringResource(R.string.completed),
        state = filterCompleted,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterCompletedManga) },
    )

    val trackers = remember { screenModel.trackers }
    when (trackers.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackers[0]
            val filterTracker by screenModel.libraryPreferences.filterTrackedManga(
                service.id.toInt(),
            ).collectAsState()
            TriStateItem(
                label = stringResource(R.string.action_filter_tracked),
                state = filterTracker,
                onClick = { screenModel.toggleTracker(service.id.toInt()) },
            )
        }
        else -> {
            HeadingItem(R.string.action_filter_tracked)
            trackers.map { service ->
                val filterTracker by screenModel.libraryPreferences.filterTrackedManga(
                    service.id.toInt(),
                ).collectAsState()
                TriStateItem(
                    label = service.name,
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
    screenModel: MangaLibrarySettingsScreenModel,
) {
    val sortingMode = category.sort.type
    val sortDescending = !category.sort.isAscending

    val trackerSortOption =
        if (screenModel.trackers.isEmpty()) {
            emptyList()
        } else {
            listOf(R.string.action_sort_tracker_score to MangaLibrarySort.Type.TrackerMean)
        }

    listOf(
        R.string.action_sort_alpha to MangaLibrarySort.Type.Alphabetical,
        R.string.action_sort_total to MangaLibrarySort.Type.TotalChapters,
        R.string.action_sort_last_read to MangaLibrarySort.Type.LastRead,
        R.string.action_sort_last_manga_update to MangaLibrarySort.Type.LastUpdate,
        R.string.action_sort_unread_count to MangaLibrarySort.Type.UnreadCount,
        R.string.action_sort_latest_chapter to MangaLibrarySort.Type.LatestChapter,
        R.string.action_sort_chapter_fetch_date to MangaLibrarySort.Type.ChapterFetchDate,
        R.string.action_sort_date_added to MangaLibrarySort.Type.DateAdded,
    ).plus(trackerSortOption).map { (titleRes, mode) ->
        SortItem(
            label = stringResource(titleRes),
            sortDescending = sortDescending.takeIf { sortingMode == mode },
            onClick = {
                val isTogglingDirection = sortingMode == mode
                val direction = when {
                    isTogglingDirection -> if (sortDescending) {
                        MangaLibrarySort.Direction.Ascending
                    } else {
                        MangaLibrarySort.Direction.Descending
                    }
                    else -> if (sortDescending) {
                        MangaLibrarySort.Direction.Descending
                    } else {
                        MangaLibrarySort.Direction.Ascending
                    }
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
    screenModel: MangaLibrarySettingsScreenModel,
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
                screenModel.libraryPreferences.mangaLandscapeColumns()
            } else {
                screenModel.libraryPreferences.mangaPortraitColumns()
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
        label = stringResource(R.string.action_display_download_badge),
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
