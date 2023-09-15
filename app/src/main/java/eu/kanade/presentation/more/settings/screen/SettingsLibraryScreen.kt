package eu.kanade.presentation.more.settings.screen

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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

        val includedAnime by animelibUpdateCategoriesPref.collectAsState()
        val excludedAnime by animelibUpdateCategoriesExcludePref.collectAsState()
        var showAnimeDialog by rememberSaveable { mutableStateOf(false) }
        if (showAnimeDialog) {
            TriStateListDialog(
                title = stringResource(R.string.anime_categories),
                message = stringResource(R.string.pref_anime_library_update_categories_details),
                items = allAnimeCategories,
                initialChecked = includedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                initialInversed = excludedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showAnimeDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    animelibUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    animelibUpdateCategoriesExcludePref.set(
                        newExcluded.map { it.id.toString() }
                            .toSet(),
                    )
                    showAnimeDialog = false
                },
            )
        }

        val libraryUpdateCategoriesPref = libraryPreferences.mangaLibraryUpdateCategories()
        val libraryUpdateCategoriesExcludePref =
            libraryPreferences.mangaLibraryUpdateCategoriesExclude()

        val includedManga by libraryUpdateCategoriesPref.collectAsState()
        val excludedManga by libraryUpdateCategoriesExcludePref.collectAsState()
        var showMangaDialog by rememberSaveable { mutableStateOf(false) }
        if (showMangaDialog) {
            TriStateListDialog(
                title = stringResource(R.string.manga_categories),
                message = stringResource(R.string.pref_manga_library_update_categories_details),
                items = allMangaCategories,
                initialChecked = includedManga.mapNotNull { id -> allMangaCategories.find { it.id.toString() == id } },
                initialInversed = excludedManga.mapNotNull { id -> allMangaCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showMangaDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    libraryUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    libraryUpdateCategoriesExcludePref.set(
                        newExcluded.map { it.id.toString() }
                            .toSet(),
                    )
                    showMangaDialog = false
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_library_update),
            preferenceItems = listOf(
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
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = libraryUpdateMangaRestrictionPref,
                    title = stringResource(R.string.pref_library_update_manga_restriction),
                    entries = mapOf(
                        ENTRY_HAS_UNVIEWED to stringResource(R.string.pref_update_only_completely_read),
                        ENTRY_NON_VIEWED to stringResource(R.string.pref_update_only_started),
                        ENTRY_NON_COMPLETED to stringResource(R.string.pref_update_only_non_completed),
                    ),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.anime_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allAnimeCategories,
                        included = includedAnime,
                        excluded = excludedAnime,
                    ),
                    onClick = { showAnimeDialog = true },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.manga_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allMangaCategories,
                        included = includedManga,
                        excluded = excludedManga,
                    ),
                    onClick = { showMangaDialog = true },
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
}
