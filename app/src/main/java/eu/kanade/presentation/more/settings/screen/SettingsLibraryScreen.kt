package eu.kanade.presentation.more.settings.screen

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
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.ui.category.CategoriesTab
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.category.manga.interactor.ResetMangaCategoryFlags
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.anime.model.AnimeGroupLibraryMode
import tachiyomi.domain.library.manga.model.MangaGroupLibraryMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_HAS_UNVIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_VIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_OUTSIDE_RELEASE_PERIOD
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsLibraryScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetMangaCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())
        val getAnimeCategories = remember { Injekt.get<GetAnimeCategories>() }
        val allAnimeCategories by getAnimeCategories.subscribe().collectAsState(initial = emptyList())
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }

        return listOf(
            getCategoriesGroup(
                LocalNavigator.currentOrThrow,
                allCategories,
                allAnimeCategories,
                libraryPreferences,
            ),
            getGlobalUpdateGroup(allCategories, allAnimeCategories, libraryPreferences),
            getEpisodeSwipeActionsGroup(libraryPreferences),
            getChapterSwipeActionsGroup(libraryPreferences),
        )
    }

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        allAnimeCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val userCategoriesCount = allCategories.filterNot(Category::isSystemCategory).size
        val userAnimeCategoriesCount = allAnimeCategories.filterNot(Category::isSystemCategory).size

        // For default category
        val mangaIds = listOf(libraryPreferences.defaultMangaCategory().defaultValue()) +
            allCategories.fastMap { it.id.toInt() }
        val animeIds = listOf(libraryPreferences.defaultAnimeCategory().defaultValue()) +
            allAnimeCategories.fastMap { it.id.toInt() }

        val mangaLabels = listOf(stringResource(MR.strings.default_category_summary)) +
            allCategories.fastMap { it.visualName }
        val animeLabels = listOf(stringResource(MR.strings.default_category_summary)) +
            allAnimeCategories.fastMap { it.visualName }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.general_categories),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.action_edit_anime_categories),
                    subtitle = pluralStringResource(
                        MR.plurals.num_categories,
                        count = userAnimeCategoriesCount,
                        userAnimeCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoriesTab) },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.defaultAnimeCategory(),
                    title = stringResource(MR.strings.default_anime_category),
                    entries = animeIds.zip(animeLabels).toMap().toImmutableMap(),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.action_edit_manga_categories),
                    subtitle = pluralStringResource(
                        MR.plurals.num_categories,
                        count = userCategoriesCount,
                        userCategoriesCount,
                    ),
                    onClick = {
                        navigator.push(CategoriesTab)
                        CategoriesTab.showMangaCategory()
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.defaultMangaCategory(),
                    title = stringResource(MR.strings.default_manga_category),
                    entries = mangaIds.zip(mangaLabels).toMap().toImmutableMap(),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.categorizedDisplaySettings(),
                    title = stringResource(MR.strings.categorized_display_settings),
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
                    title = stringResource(MR.strings.pref_category_hide_hidden),
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

        val autoUpdateIntervalPref = libraryPreferences.autoUpdateInterval()
        val autoUpdateInterval by autoUpdateIntervalPref.collectAsState()

        val animeAutoUpdateCategoriesPref = libraryPreferences.animeUpdateCategories()
        val animeAutoUpdateCategoriesExcludePref =
            libraryPreferences.animeUpdateCategoriesExclude()

        val includedAnime by animeAutoUpdateCategoriesPref.collectAsState()
        val excludedAnime by animeAutoUpdateCategoriesExcludePref.collectAsState()
        var showAnimeCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showAnimeCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.anime_categories),
                message = stringResource(MR.strings.pref_anime_library_update_categories_details),
                items = allAnimeCategories,
                initialChecked = includedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                initialInversed = excludedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showAnimeCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    animeAutoUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    animeAutoUpdateCategoriesExcludePref.set(
                        newExcluded.map { it.id.toString() }
                            .toSet(),
                    )
                    showAnimeCategoriesDialog = false
                },
            )
        }

        val autoUpdateCategoriesPref = libraryPreferences.mangaUpdateCategories()
        val autoUpdateCategoriesExcludePref =
            libraryPreferences.mangaUpdateCategoriesExclude()

        val includedManga by autoUpdateCategoriesPref.collectAsState()
        val excludedManga by autoUpdateCategoriesExcludePref.collectAsState()
        var showMangaCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showMangaCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.manga_categories),
                message = stringResource(MR.strings.pref_manga_library_update_categories_details),
                items = allMangaCategories,
                initialChecked = includedManga.mapNotNull { id -> allMangaCategories.find { it.id.toString() == id } },
                initialInversed = excludedManga.mapNotNull { id -> allMangaCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showMangaCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    autoUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    autoUpdateCategoriesExcludePref.set(
                        newExcluded.map { it.id.toString() }
                            .toSet(),
                    )
                    showMangaCategoriesDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_library_update),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    pref = autoUpdateIntervalPref,
                    title = stringResource(MR.strings.pref_library_update_interval),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.update_never),
                        12 to stringResource(MR.strings.update_12hour),
                        24 to stringResource(MR.strings.update_24hour),
                        48 to stringResource(MR.strings.update_48hour),
                        72 to stringResource(MR.strings.update_72hour),
                        168 to stringResource(MR.strings.update_weekly),
                    ),
                    onValueChanged = {
                        MangaLibraryUpdateJob.setupTask(context, it)
                        AnimeLibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = libraryPreferences.autoUpdateDeviceRestrictions(),
                    enabled = autoUpdateInterval > 0,
                    title = stringResource(MR.strings.pref_library_update_restriction),
                    subtitle = stringResource(MR.strings.restrictions),
                    entries = persistentMapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                        DEVICE_CHARGING to stringResource(MR.strings.charging),
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
                    title = stringResource(MR.strings.anime_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allAnimeCategories,
                        included = includedAnime,
                        excluded = excludedAnime,
                    ),
                    onClick = { showAnimeCategoriesDialog = true },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.manga_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allMangaCategories,
                        included = includedManga,
                        excluded = excludedManga,
                    ),
                    onClick = { showMangaCategoriesDialog = true },
                ),
                // SY -->
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.groupAnimeLibraryUpdateType(),
                    title = stringResource(MR.strings.anime_library_group_updates),
                    entries = persistentMapOf(
                        AnimeGroupLibraryMode.GLOBAL to stringResource(
                            MR.strings.library_group_updates_global,
                        ),
                        AnimeGroupLibraryMode.ALL_BUT_UNGROUPED to stringResource(
                            MR.strings.library_group_updates_all_but_ungrouped,
                        ),
                        AnimeGroupLibraryMode.ALL to stringResource(
                            MR.strings.library_group_updates_all,
                        ),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.groupMangaLibraryUpdateType(),
                    title = stringResource(MR.strings.manga_library_group_updates),
                    entries = persistentMapOf(
                        MangaGroupLibraryMode.GLOBAL to stringResource(
                            MR.strings.library_group_updates_global,
                        ),
                        MangaGroupLibraryMode.ALL_BUT_UNGROUPED to stringResource(
                            MR.strings.library_group_updates_all_but_ungrouped,
                        ),
                        MangaGroupLibraryMode.ALL to stringResource(
                            MR.strings.library_group_updates_all,
                        ),
                    ),
                ),
                // SY <--
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.autoUpdateMetadata(),
                    title = stringResource(MR.strings.pref_library_update_refresh_metadata),
                    subtitle = stringResource(MR.strings.pref_library_update_refresh_metadata_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = libraryPreferences.autoUpdateItemRestrictions(),
                    title = stringResource(MR.strings.pref_library_update_smart_update),
                    entries = persistentMapOf(
                        ENTRY_HAS_UNVIEWED to stringResource(MR.strings.pref_update_only_completely_read),
                        ENTRY_NON_VIEWED to stringResource(MR.strings.pref_update_only_started),
                        ENTRY_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                        ENTRY_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.newShowUpdatesCount(),
                    title = stringResource(MR.strings.pref_library_update_show_tab_badge),
                ),
            ),
        )
    }

    @Composable
    private fun getChapterSwipeActionsGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_chapter_swipe),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeChapterStartAction(),
                    title = stringResource(MR.strings.pref_chapter_swipe_start),
                    entries = persistentMapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeChapterEndAction(),
                    title = stringResource(MR.strings.pref_chapter_swipe_end),
                    entries = persistentMapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
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
            title = stringResource(MR.strings.pref_episode_swipe),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeEpisodeStartAction(),
                    title = stringResource(MR.strings.pref_episode_swipe_start),
                    entries = persistentMapOf(
                        LibraryPreferences.EpisodeSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleSeen to
                            stringResource(MR.strings.action_mark_as_seen),
                        LibraryPreferences.EpisodeSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.swipeEpisodeEndAction(),
                    title = stringResource(MR.strings.pref_episode_swipe_end),
                    entries = persistentMapOf(
                        LibraryPreferences.EpisodeSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleSeen to
                            stringResource(MR.strings.action_mark_as_seen),
                        LibraryPreferences.EpisodeSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                ),
            ),
        )
    }
}
