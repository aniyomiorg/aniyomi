package eu.kanade.presentation.more.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.category.CategoriesTab
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.category.manga.interactor.ResetMangaCategoryFlags
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_BATTERY_NOT_LOW
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_HAS_UNVIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_VIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_OUTSIDE_RELEASE_PERIOD
import tachiyomi.presentation.core.components.WheelTextPicker
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsLibraryScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    @StringRes
    override fun getTitleRes() = R.string.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetMangaCategories>() }
        val allCategories by getCategories.subscribe()
            .collectAsState(initial = runBlocking { getCategories.await() })
        val getAnimeCategories = remember { Injekt.get<GetAnimeCategories>() }
        val allAnimeCategories by getAnimeCategories.subscribe()
            .collectAsState(initial = runBlocking { getAnimeCategories.await() })
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }

        return listOf(
            getCategoriesGroup(
                LocalNavigator.currentOrThrow,
                allCategories,
                allAnimeCategories,
                libraryPreferences,
            ),
            getGlobalUpdateGroup(allCategories, allAnimeCategories, libraryPreferences),
            getChapterSwipeActionsGroup(libraryPreferences),
            getEpisodeSwipeActionsGroup(libraryPreferences),
        )
    }

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        allAnimeCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val userCategoriesCount = allCategories.filterNot(Category::isSystemCategory).size
        val userAnimeCategoriesCount = allAnimeCategories.filterNot(Category::isSystemCategory).size

        val defaultCategory by libraryPreferences.defaultMangaCategory().collectAsState()
        val selectedCategory = allCategories.find { it.id == defaultCategory.toLong() }
        val defaultAnimeCategory by libraryPreferences.defaultAnimeCategory().collectAsState()
        val selectedAnimeCategory =
            allAnimeCategories.find { it.id == defaultAnimeCategory.toLong() }

        // For default category
        val mangaIds = listOf(libraryPreferences.defaultMangaCategory().defaultValue()) +
            allCategories.fastMap { it.id.toInt() }
        val animeIds = listOf(libraryPreferences.defaultAnimeCategory().defaultValue()) +
            allAnimeCategories.fastMap { it.id.toInt() }

        val mangaLabels = listOf(stringResource(R.string.default_category_summary)) +
            allCategories.fastMap { it.visualName(context) }
        val animeLabels = listOf(stringResource(R.string.default_category_summary)) +
            allAnimeCategories.fastMap { it.visualName(context) }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.general_categories),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.action_edit_anime_categories),
                    subtitle = pluralStringResource(
                        id = R.plurals.num_categories,
                        count = userAnimeCategoriesCount,
                        userAnimeCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoriesTab(false)) },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.defaultAnimeCategory(),
                    title = stringResource(R.string.default_anime_category),
                    subtitle = selectedAnimeCategory?.visualName
                        ?: stringResource(R.string.default_category_summary),
                    entries = animeIds.zip(animeLabels).toMap(),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.action_edit_manga_categories),
                    subtitle = pluralStringResource(
                        id = R.plurals.num_categories,
                        count = userCategoriesCount,
                        userCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoriesTab(true)) },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.defaultMangaCategory(),
                    title = stringResource(R.string.default_manga_category),
                    subtitle = selectedCategory?.visualName
                        ?: stringResource(R.string.default_category_summary),
                    entries = mangaIds.zip(mangaLabels).toMap(),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.categorizedDisplaySettings(),
                    title = stringResource(R.string.categorized_display_settings),
                    onValueChanged = {
                        if (!it) {
                            scope.launch {
                                Injekt.get<ResetMangaCategoryFlags>().await()
                            }
                        }
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.hideHiddenCategoriesSettings(),
                    title = stringResource(R.string.pref_category_hide_hidden),
                ),
            ),
        )
    }

    @Composable
    private fun getGlobalUpdateGroup(
        allMangaCategories: List<Category>,
        allAnimeCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val libraryUpdateIntervalPref = libraryPreferences.libraryUpdateInterval()
        val libraryUpdateInterval by libraryUpdateIntervalPref.collectAsState()
        val libraryUpdateDeviceRestrictionPref = libraryPreferences.libraryUpdateDeviceRestriction()
        val libraryUpdateMangaRestrictionPref = libraryPreferences.libraryUpdateItemRestriction()

        val animelibUpdateCategoriesPref = libraryPreferences.animeLibraryUpdateCategories()
        val animelibUpdateCategoriesExcludePref =
            libraryPreferences.animeLibraryUpdateCategoriesExclude()
        val libraryUpdateAnimeRestriction by libraryUpdateMangaRestrictionPref.collectAsState()

        val includedAnime by animelibUpdateCategoriesPref.collectAsState()
        val excludedAnime by animelibUpdateCategoriesExcludePref.collectAsState()
        var showAnimeCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showAnimeCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(R.string.anime_categories),
                message = stringResource(R.string.pref_anime_library_update_categories_details),
                items = allAnimeCategories,
                initialChecked = includedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                initialInversed = excludedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showAnimeCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    animelibUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    animelibUpdateCategoriesExcludePref.set(
                        newExcluded.map { it.id.toString() }
                            .toSet(),
                    )
                    showAnimeCategoriesDialog = false
                },
            )
        }
        val leadAnimeRange by libraryPreferences.leadingAnimeExpectedDays().collectAsState()
        val followAnimeRange by libraryPreferences.followingAnimeExpectedDays().collectAsState()

        var showFetchAnimeRangesDialog by rememberSaveable { mutableStateOf(false) }
        if (showFetchAnimeRangesDialog) {
            LibraryExpectedRangeDialog(
                initialLead = leadAnimeRange,
                initialFollow = followAnimeRange,
                onDismissRequest = { showFetchAnimeRangesDialog = false },
                onValueChanged = { leadValue, followValue ->
                    libraryPreferences.leadingAnimeExpectedDays().set(leadValue)
                    libraryPreferences.followingAnimeExpectedDays().set(followValue)
                    showFetchAnimeRangesDialog = false
                },
            )
        }

        val libraryUpdateCategoriesPref = libraryPreferences.mangaLibraryUpdateCategories()
        val libraryUpdateCategoriesExcludePref =
            libraryPreferences.mangaLibraryUpdateCategoriesExclude()
        val libraryUpdateMangaRestriction by libraryUpdateMangaRestrictionPref.collectAsState()

        val includedManga by libraryUpdateCategoriesPref.collectAsState()
        val excludedManga by libraryUpdateCategoriesExcludePref.collectAsState()
        var showMangaCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showMangaCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(R.string.manga_categories),
                message = stringResource(R.string.pref_manga_library_update_categories_details),
                items = allMangaCategories,
                initialChecked = includedManga.mapNotNull { id -> allMangaCategories.find { it.id.toString() == id } },
                initialInversed = excludedManga.mapNotNull { id -> allMangaCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showMangaCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    libraryUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    libraryUpdateCategoriesExcludePref.set(
                        newExcluded.map { it.id.toString() }
                            .toSet(),
                    )
                    showMangaCategoriesDialog = false
                },
            )
        }
        val leadMangaRange by libraryPreferences.leadingMangaExpectedDays().collectAsState()
        val followMangaRange by libraryPreferences.followingMangaExpectedDays().collectAsState()

        var showFetchMangaRangesDialog by rememberSaveable { mutableStateOf(false) }
        if (showFetchMangaRangesDialog) {
            LibraryExpectedRangeDialog(
                initialLead = leadMangaRange,
                initialFollow = followMangaRange,
                onDismissRequest = { showFetchMangaRangesDialog = false },
                onValueChanged = { leadValue, followValue ->
                    libraryPreferences.leadingMangaExpectedDays().set(leadValue)
                    libraryPreferences.followingMangaExpectedDays().set(followValue)
                    showFetchMangaRangesDialog = false
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_library_update),
            preferenceItems = listOfNotNull(
                Preference.PreferenceItem.ListPreference(
                    pref = libraryUpdateIntervalPref,
                    title = stringResource(R.string.pref_library_update_interval),
                    entries = mapOf(
                        0 to stringResource(R.string.update_never),
                        12 to stringResource(R.string.update_12hour),
                        24 to stringResource(R.string.update_24hour),
                        48 to stringResource(R.string.update_48hour),
                        72 to stringResource(R.string.update_72hour),
                        168 to stringResource(R.string.update_weekly),
                    ),
                    onValueChanged = {
                        MangaLibraryUpdateJob.setupTask(context, it)
                        AnimeLibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = libraryUpdateDeviceRestrictionPref,
                    enabled = libraryUpdateInterval > 0,
                    title = stringResource(R.string.pref_library_update_restriction),
                    subtitle = stringResource(R.string.restrictions),
                    entries = mapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(R.string.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(R.string.network_not_metered),
                        DEVICE_CHARGING to stringResource(R.string.charging),
                        DEVICE_BATTERY_NOT_LOW to stringResource(R.string.battery_not_low),
                    ),
                    onValueChanged = {
                        // Post to event looper to allow the preference to be updated.
                        ContextCompat.getMainExecutor(context).execute {
                            MangaLibraryUpdateJob.setupTask(context)
                            AnimeLibraryUpdateJob.setupTask(context)
                        }
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.anime_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allAnimeCategories,
                        included = includedAnime,
                        excluded = excludedAnime,
                    ),
                    onClick = { showAnimeCategoriesDialog = true },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.manga_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allMangaCategories,
                        included = includedManga,
                        excluded = excludedManga,
                    ),
                    onClick = { showMangaCategoriesDialog = true },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.autoUpdateMetadata(),
                    title = stringResource(R.string.pref_library_update_refresh_metadata),
                    subtitle = stringResource(R.string.pref_library_update_refresh_metadata_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.autoUpdateTrackers(),
                    enabled = Injekt.get<TrackManager>().hasLoggedServices(),
                    title = stringResource(R.string.pref_library_update_refresh_trackers),
                    subtitle = stringResource(R.string.pref_library_update_refresh_trackers_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = libraryUpdateMangaRestrictionPref,
                    title = stringResource(R.string.pref_library_update_manga_restriction),
                    entries = mapOf(
                        ENTRY_HAS_UNVIEWED to stringResource(R.string.pref_update_only_completely_read),
                        ENTRY_NON_VIEWED to stringResource(R.string.pref_update_only_started),
                        ENTRY_NON_COMPLETED to stringResource(R.string.pref_update_only_non_completed),
                        ENTRY_OUTSIDE_RELEASE_PERIOD to stringResource(R.string.pref_update_only_in_release_period),
                    ),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_update_release_grace_period),
                    subtitle = listOf(
                        pluralStringResource(R.plurals.pref_update_release_leading_days, leadMangaRange, leadMangaRange),
                        pluralStringResource(R.plurals.pref_update_release_following_days, followMangaRange, followMangaRange),
                    ).joinToString(),
                    onClick = { showFetchMangaRangesDialog = true },
                ).takeIf { ENTRY_OUTSIDE_RELEASE_PERIOD in libraryUpdateMangaRestriction },
                Preference.PreferenceItem.InfoPreference(
                    title = stringResource(R.string.pref_update_release_grace_period_info),
                ).takeIf { ENTRY_OUTSIDE_RELEASE_PERIOD in libraryUpdateMangaRestriction },

                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_update_anime_release_grace_period),
                    subtitle = listOf(
                        pluralStringResource(R.plurals.pref_update_release_leading_days, leadAnimeRange, leadAnimeRange),
                        pluralStringResource(R.plurals.pref_update_release_following_days, followAnimeRange, followAnimeRange),
                    ).joinToString(),
                    onClick = { showFetchAnimeRangesDialog = true },
                ).takeIf { ENTRY_OUTSIDE_RELEASE_PERIOD in libraryUpdateAnimeRestriction },
                Preference.PreferenceItem.InfoPreference(
                    title = stringResource(R.string.pref_update_release_grace_period_info),
                ).takeIf { ENTRY_OUTSIDE_RELEASE_PERIOD in libraryUpdateAnimeRestriction },
            ),
        )
    }

    @Composable
    private fun getChapterSwipeActionsGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_chapter_swipe),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeChapterStartAction(),
                    title = stringResource(R.string.pref_chapter_swipe_start),
                    entries = mapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to stringResource(R.string.action_disable),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to stringResource(R.string.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to stringResource(R.string.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to stringResource(R.string.action_download),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeChapterEndAction(),
                    title = stringResource(R.string.pref_chapter_swipe_end),
                    entries = mapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to stringResource(R.string.action_disable),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to stringResource(R.string.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to stringResource(R.string.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to stringResource(R.string.action_download),
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun getEpisodeSwipeActionsGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_episode_swipe),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeEpisodeStartAction(),
                    title = stringResource(R.string.pref_episode_swipe_start),
                    entries = mapOf(
                        LibraryPreferences.EpisodeSwipeAction.Disabled to stringResource(R.string.action_disable),
                        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark to stringResource(R.string.action_bookmark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleSeen to stringResource(R.string.action_mark_as_seen),
                        LibraryPreferences.EpisodeSwipeAction.Download to stringResource(R.string.action_download),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeEpisodeEndAction(),
                    title = stringResource(R.string.pref_episode_swipe_end),
                    entries = mapOf(
                        LibraryPreferences.EpisodeSwipeAction.Disabled to stringResource(R.string.action_disable),
                        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark to stringResource(R.string.action_bookmark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleSeen to stringResource(R.string.action_mark_as_seen),
                        LibraryPreferences.EpisodeSwipeAction.Download to stringResource(R.string.action_download),
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun LibraryExpectedRangeDialog(
        initialLead: Int,
        initialFollow: Int,
        onDismissRequest: () -> Unit,
        onValueChanged: (portrait: Int, landscape: Int) -> Unit,
    ) {
        var leadValue by rememberSaveable { mutableIntStateOf(initialLead) }
        var followValue by rememberSaveable { mutableIntStateOf(initialFollow) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(R.string.pref_update_release_grace_period)) },
            text = {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = pluralStringResource(R.plurals.pref_update_release_leading_days, leadValue, leadValue),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = pluralStringResource(R.plurals.pref_update_release_following_days, followValue, followValue),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val size = DpSize(width = maxWidth / 2, height = 128.dp)
                    val items = (0..28).map {
                        if (it == 0) {
                            stringResource(R.string.label_default)
                        } else {
                            it.toString()
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WheelTextPicker(
                            size = size,
                            items = items,
                            startIndex = leadValue,
                            onSelectionChanged = {
                                leadValue = it
                            },
                        )
                        WheelTextPicker(
                            size = size,
                            items = items,
                            startIndex = followValue,
                            onSelectionChanged = {
                                followValue = it
                            },
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChanged(leadValue, followValue) }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }
}
