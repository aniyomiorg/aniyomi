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
import eu.kanade.presentation.more.settings.PreferenceItem
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
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_HAS_UNVIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_VIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_NEW
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_EPISODE_SEEN_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_EPISODE_SEEN_NEW
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
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
            getSeasonBehaviorGroup(libraryPreferences),
            getAnimeBehaviorGroup(libraryPreferences),
            getBehaviorGroup(libraryPreferences),
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
            title = stringResource(AYMR.strings.general_categories),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(AYMR.strings.action_edit_anime_categories),
                    subtitle = pluralStringResource(
                        MR.plurals.num_categories,
                        count = userAnimeCategoriesCount,
                        userAnimeCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoriesTab) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.defaultAnimeCategory(),
                    entries = animeIds.zip(animeLabels).toMap().toImmutableMap(),
                    title = stringResource(AYMR.strings.default_anime_category),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(AYMR.strings.action_edit_manga_categories),
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
                    preference = libraryPreferences.defaultMangaCategory(),
                    entries = mangaIds.zip(mangaLabels).toMap().toImmutableMap(),
                    title = stringResource(AYMR.strings.default_manga_category),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categorizedDisplaySettings(),
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
                    preference = libraryPreferences.hideHiddenCategoriesSettings(),
                    title = stringResource(AYMR.strings.pref_category_hide_hidden),
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
                title = stringResource(AYMR.strings.anime_categories),
                message = stringResource(AYMR.strings.pref_anime_library_update_categories_details),
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
                title = stringResource(AYMR.strings.manga_categories),
                message = stringResource(AYMR.strings.pref_manga_library_update_categories_details),
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
                    preference = autoUpdateIntervalPref,
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.update_never),
                        12 to stringResource(MR.strings.update_12hour),
                        24 to stringResource(MR.strings.update_24hour),
                        48 to stringResource(MR.strings.update_48hour),
                        72 to stringResource(MR.strings.update_72hour),
                        168 to stringResource(MR.strings.update_weekly),
                    ),
                    title = stringResource(MR.strings.pref_library_update_interval),
                    onValueChanged = {
                        MangaLibraryUpdateJob.setupTask(context, it)
                        AnimeLibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateDeviceRestrictions(),
                    entries = persistentMapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                        DEVICE_CHARGING to stringResource(MR.strings.charging),
                    ),
                    title = stringResource(MR.strings.pref_library_update_restriction),
                    subtitle = stringResource(MR.strings.restrictions),
                    enabled = autoUpdateInterval > 0,
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
                    title = stringResource(AYMR.strings.anime_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allAnimeCategories,
                        included = includedAnime,
                        excluded = excludedAnime,
                    ),
                    onClick = { showAnimeCategoriesDialog = true },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(AYMR.strings.manga_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allMangaCategories,
                        included = includedManga,
                        excluded = excludedManga,
                    ),
                    onClick = { showMangaCategoriesDialog = true },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.autoUpdateMetadata(),
                    title = stringResource(MR.strings.pref_library_update_refresh_metadata),
                    subtitle = stringResource(MR.strings.pref_library_update_refresh_metadata_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateItemRestrictions(),
                    entries = persistentMapOf(
                        ENTRY_HAS_UNVIEWED to stringResource(AYMR.strings.pref_update_only_completely_read),
                        ENTRY_NON_VIEWED to stringResource(MR.strings.pref_update_only_started),
                        ENTRY_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                        ENTRY_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                    ),
                    title = stringResource(MR.strings.pref_library_update_smart_update),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.newShowUpdatesCount(),
                    title = stringResource(AYMR.strings.pref_library_update_show_tab_badge),
                ),
            ),
        )
    }

    @Composable
    private fun getSeasonBehaviorGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_library_season),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.updateSeasonOnRefresh(),
                    title = stringResource(AYMR.strings.pref_update_seasons_refresh),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.updateSeasonOnLibraryUpdate(),
                    title = stringResource(AYMR.strings.pref_update_seasons_update),
                ),
            ),
        )
    }

    @Composable
    private fun getBehaviorGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_behavior),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeChapterStartAction(),
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
                    title = stringResource(MR.strings.pref_chapter_swipe_start),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeChapterEndAction(),
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
                    title = stringResource(MR.strings.pref_chapter_swipe_end),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.markDuplicateReadChapterAsRead(),
                    entries = persistentMapOf(
                        MARK_DUPLICATE_CHAPTER_READ_EXISTING to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_existing),
                        MARK_DUPLICATE_CHAPTER_READ_NEW to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_new),
                    ),
                    title = stringResource(MR.strings.pref_mark_duplicate_read_chapter_read),
                ),
            ),
        )
    }

    @Composable
    private fun getAnimeBehaviorGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_behavior_episode),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeEpisodeStartAction(),
                    entries = persistentMapOf(
                        LibraryPreferences.EpisodeSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark to
                            stringResource(AYMR.strings.action_bookmark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleFillermark to
                            stringResource(AYMR.strings.action_fillermark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleSeen to
                            stringResource(AYMR.strings.action_mark_as_seen),
                        LibraryPreferences.EpisodeSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(AYMR.strings.pref_episode_swipe_start),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeEpisodeEndAction(),
                    entries = persistentMapOf(
                        LibraryPreferences.EpisodeSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark to
                            stringResource(AYMR.strings.action_bookmark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleFillermark to
                            stringResource(AYMR.strings.action_fillermark_episode),
                        LibraryPreferences.EpisodeSwipeAction.ToggleSeen to
                            stringResource(AYMR.strings.action_mark_as_seen),
                        LibraryPreferences.EpisodeSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(AYMR.strings.pref_episode_swipe_end),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.markDuplicateSeenEpisodeAsSeen(),
                    entries = persistentMapOf(
                        MARK_DUPLICATE_EPISODE_SEEN_EXISTING to
                            stringResource(AYMR.strings.pref_mark_duplicate_seen_episode_seen_existing),
                        MARK_DUPLICATE_EPISODE_SEEN_NEW to
                            stringResource(AYMR.strings.pref_mark_duplicate_seen_episode_seen_new),
                    ),
                    title = stringResource(AYMR.strings.pref_mark_duplicate_seen_episode_seen),
                ),
            ),
        )
    }
}
