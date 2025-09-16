package tachiyomi.domain.library.service

import aniyomi.domain.anime.SeasonDisplayMode
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.manga.model.MangaLibrarySort
import tachiyomi.domain.library.model.LibraryDisplayMode

class LibraryPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun displayMode() = preferenceStore.getObject(
        "pref_display_mode_library",
        LibraryDisplayMode.default,
        LibraryDisplayMode.Serializer::serialize,
        LibraryDisplayMode.Serializer::deserialize,
    )

    fun mangaSortingMode() = preferenceStore.getObject(
        "library_sorting_mode",
        MangaLibrarySort.default,
        MangaLibrarySort.Serializer::serialize,
        MangaLibrarySort.Serializer::deserialize,
    )

    fun animeSortingMode() = preferenceStore.getObject(
        "animelib_sorting_mode",
        AnimeLibrarySort.default,
        AnimeLibrarySort.Serializer::serialize,
        AnimeLibrarySort.Serializer::deserialize,
    )

    fun lastUpdatedTimestamp() = preferenceStore.getLong(Preference.appStateKey("library_update_last_timestamp"), 0L)
    fun autoUpdateInterval() = preferenceStore.getInt("pref_library_update_interval_key", 0)

    fun autoUpdateDeviceRestrictions() = preferenceStore.getStringSet(
        "library_update_restriction",
        setOf(
            DEVICE_ONLY_ON_WIFI,
        ),
    )

    fun autoUpdateItemRestrictions() = preferenceStore.getStringSet(
        "library_update_manga_restriction",
        setOf(
            ENTRY_HAS_UNVIEWED,
            ENTRY_NON_COMPLETED,
            ENTRY_NON_VIEWED,
            ENTRY_OUTSIDE_RELEASE_PERIOD,
        ),
    )

    fun autoUpdateMetadata() = preferenceStore.getBoolean("auto_update_metadata", false)

    fun showContinueViewingButton() =
        preferenceStore.getBoolean("display_continue_reading_button", false)

    // Common Category

    fun categoryTabs() = preferenceStore.getBoolean("display_category_tabs", true)

    fun categoryNumberOfItems() = preferenceStore.getBoolean("display_number_of_items", false)

    fun categorizedDisplaySettings() = preferenceStore.getBoolean("categorized_display", false)

    fun hideHiddenCategoriesSettings() = preferenceStore.getBoolean("hidden_categories", false)

    fun filterIntervalCustom() = preferenceStore.getEnum(
        "pref_filter_library_interval_custom",
        TriState.DISABLED,
    )

    // Common badges

    fun downloadBadge() = preferenceStore.getBoolean("display_download_badge", false)

    fun unreadBadge() = preferenceStore.getBoolean("display_unread_badge", true)

    fun localBadge() = preferenceStore.getBoolean("display_local_badge", true)

    fun languageBadge() = preferenceStore.getBoolean("display_language_badge", false)

    fun newShowUpdatesCount() = preferenceStore.getBoolean("library_show_updates_count", true)

    // Common Cache

    fun autoClearItemCache() = preferenceStore.getBoolean("auto_clear_chapter_cache", false)

    // Random Sort Seed

    fun randomAnimeSortSeed() = preferenceStore.getInt("library_random_anime_sort_seed", 0)
    fun randomMangaSortSeed() = preferenceStore.getInt("library_random_manga_sort_seed", 0)

    // Mixture Columns

    fun animePortraitColumns() = preferenceStore.getInt("pref_animelib_columns_portrait_key", 0)
    fun mangaPortraitColumns() = preferenceStore.getInt("pref_library_columns_portrait_key", 0)

    fun animeLandscapeColumns() = preferenceStore.getInt("pref_animelib_columns_landscape_key", 0)
    fun mangaLandscapeColumns() = preferenceStore.getInt("pref_library_columns_landscape_key", 0)

    // Mixture Filter

    fun filterDownloadedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_downloaded_v2", TriState.DISABLED)

    fun filterDownloadedManga() =
        preferenceStore.getEnum("pref_filter_library_downloaded_v2", TriState.DISABLED)

    fun filterUnseen() =
        preferenceStore.getEnum("pref_filter_animelib_unread_v2", TriState.DISABLED)

    fun filterUnread() =
        preferenceStore.getEnum("pref_filter_library_unread_v2", TriState.DISABLED)

    fun filterStartedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_started_v2", TriState.DISABLED)

    fun filterStartedManga() =
        preferenceStore.getEnum("pref_filter_library_started_v2", TriState.DISABLED)

    fun filterBookmarkedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_bookmarked_v2", TriState.DISABLED)

    fun filterBookmarkedManga() =
        preferenceStore.getEnum("pref_filter_library_bookmarked_v2", TriState.DISABLED)

    fun filterCompletedAnime() =
        preferenceStore.getEnum("pref_filter_animelib_completed_v2", TriState.DISABLED)

    fun filterCompletedManga() =
        preferenceStore.getEnum("pref_filter_library_completed_v2", TriState.DISABLED)

    fun filterTrackedAnime(id: Int) =
        preferenceStore.getEnum("pref_filter_animelib_tracked_${id}_v2", TriState.DISABLED)

    fun filterTrackedManga(id: Int) =
        preferenceStore.getEnum("pref_filter_library_tracked_${id}_v2", TriState.DISABLED)

    // Mixture Update Count

    fun newMangaUpdatesCount() = preferenceStore.getInt("library_unread_updates_count", 0)
    fun newAnimeUpdatesCount() = preferenceStore.getInt("library_unseen_updates_count", 0)

    // Mixture Category

    fun defaultAnimeCategory() = preferenceStore.getInt(DEFAULT_ANIME_CATEGORY_PREF_KEY, -1)
    fun defaultMangaCategory() = preferenceStore.getInt(DEFAULT_MANGA_CATEGORY_PREF_KEY, -1)

    fun lastUsedAnimeCategory() = preferenceStore.getInt(Preference.appStateKey("last_used_anime_category"), 0)
    fun lastUsedMangaCategory() = preferenceStore.getInt(Preference.appStateKey("last_used_category"), 0)

    fun animeUpdateCategories() =
        preferenceStore.getStringSet(LIBRARY_UPDATE_ANIME_CATEGORIES_PREF_KEY, emptySet())

    fun mangaUpdateCategories() =
        preferenceStore.getStringSet(LIBRARY_UPDATE_MANGA_CATEGORIES_PREF_KEY, emptySet())

    fun animeUpdateCategoriesExclude() =
        preferenceStore.getStringSet(LIBRARY_UPDATE_ANIME_CATEGORIES_EXCLUDE_PREF_KEY, emptySet())

    fun mangaUpdateCategoriesExclude() =
        preferenceStore.getStringSet(LIBRARY_UPDATE_MANGA_CATEGORIES_EXCLUDE_PREF_KEY, emptySet())

    // Mixture Item

    fun filterEpisodeBySeen() =
        preferenceStore.getLong("default_episode_filter_by_seen", Anime.SHOW_ALL)

    fun filterChapterByRead() =
        preferenceStore.getLong("default_chapter_filter_by_read", Manga.SHOW_ALL)

    fun filterEpisodeByDownloaded() =
        preferenceStore.getLong("default_episode_filter_by_downloaded", Anime.SHOW_ALL)

    fun filterChapterByDownloaded() =
        preferenceStore.getLong("default_chapter_filter_by_downloaded", Manga.SHOW_ALL)

    fun filterEpisodeByBookmarked() =
        preferenceStore.getLong("default_episode_filter_by_bookmarked", Anime.SHOW_ALL)

    fun filterChapterByBookmarked() =
        preferenceStore.getLong("default_chapter_filter_by_bookmarked", Manga.SHOW_ALL)

    fun filterEpisodeByFillermarked() =
        preferenceStore.getLong("default_episode_filter_by_fillermarked", Anime.SHOW_ALL)

    // and upload date
    fun sortEpisodeBySourceOrNumber() = preferenceStore.getLong(
        "default_episode_sort_by_source_or_number",
        Anime.EPISODE_SORTING_SOURCE,
    )

    fun sortChapterBySourceOrNumber() = preferenceStore.getLong(
        "default_chapter_sort_by_source_or_number",
        Manga.CHAPTER_SORTING_SOURCE,
    )

    fun displayEpisodeByNameOrNumber() = preferenceStore.getLong(
        "default_chapter_display_by_name_or_number",
        Anime.EPISODE_DISPLAY_NAME,
    )

    fun displayChapterByNameOrNumber() = preferenceStore.getLong(
        "default_chapter_display_by_name_or_number",
        Manga.CHAPTER_DISPLAY_NAME,
    )

    fun sortEpisodeByAscendingOrDescending() = preferenceStore.getLong(
        "default_chapter_sort_by_ascending_or_descending",
        Anime.EPISODE_SORT_DESC,
    )

    fun sortChapterByAscendingOrDescending() = preferenceStore.getLong(
        "default_chapter_sort_by_ascending_or_descending",
        Manga.CHAPTER_SORT_DESC,
    )

    fun showEpisodeThumbnailPreviews() = preferenceStore.getLong(
        "default_episode_show_thumbnail_previews",
        Anime.EPISODE_SHOW_PREVIEWS,
    )

    fun showEpisodeSummaries() = preferenceStore.getLong(
        "default_episode_show_summaries",
        Anime.EPISODE_SHOW_SUMMARIES,
    )

    fun setEpisodeSettingsDefault(anime: Anime) {
        filterEpisodeBySeen().set(anime.unseenFilterRaw)
        filterEpisodeByDownloaded().set(anime.downloadedFilterRaw)
        filterEpisodeByBookmarked().set(anime.bookmarkedFilterRaw)
        filterEpisodeByFillermarked().set(anime.fillermarkedFilterRaw)
        sortEpisodeBySourceOrNumber().set(anime.sorting)
        displayEpisodeByNameOrNumber().set(anime.displayMode)
        sortEpisodeByAscendingOrDescending().set(
            if (anime.sortDescending()) Anime.EPISODE_SORT_DESC else Anime.EPISODE_SORT_ASC,
        )
        showEpisodeThumbnailPreviews().set(anime.showPreviewsRaw)
        showEpisodeSummaries().set(anime.showSummariesRaw)
    }

    fun setChapterSettingsDefault(manga: Manga) {
        filterChapterByRead().set(manga.unreadFilterRaw)
        filterChapterByDownloaded().set(manga.downloadedFilterRaw)
        filterChapterByBookmarked().set(manga.bookmarkedFilterRaw)
        sortChapterBySourceOrNumber().set(manga.sorting)
        displayChapterByNameOrNumber().set(manga.displayMode)
        sortChapterByAscendingOrDescending().set(
            if (manga.sortDescending()) Manga.CHAPTER_SORT_DESC else Manga.CHAPTER_SORT_ASC,
        )
    }

    // Seasons

    fun filterSeasonByDownload() =
        preferenceStore.getLong("default_season_filter_by_downloaded", Anime.SHOW_ALL)

    fun filterSeasonByUnseen() =
        preferenceStore.getLong("default_season_filter_by_unseen", Anime.SHOW_ALL)

    fun filterSeasonByStarted() =
        preferenceStore.getLong("default_season_filter_by_started", Anime.SHOW_ALL)

    fun filterSeasonByCompleted() =
        preferenceStore.getLong("default_season_filter_by_completed", Anime.SHOW_ALL)

    fun filterSeasonByBookmarked() =
        preferenceStore.getLong("default_season_filter_by_bookmarked", Anime.SHOW_ALL)

    fun filterSeasonByFillermarked() =
        preferenceStore.getLong("default_season_filter_by_fillermarked", Anime.SHOW_ALL)

    fun sortSeasonBySourceOrNumber() = preferenceStore.getLong(
        "default_season_sort_by_source_or_number",
        Anime.SEASON_SORT_SOURCE,
    )

    fun sortSeasonByAscendingOrDescending() = preferenceStore.getLong(
        "default_season_sort_by_ascending_or_descending",
        Anime.SEASON_SORT_DESC,
    )

    fun seasonDisplayGridMode() = preferenceStore.getLong(
        "default_season_grid_display_mode",
        SeasonDisplayMode.toLong(SeasonDisplayMode.CompactGrid),
    )

    fun seasonDisplayGridSize() = preferenceStore.getInt(
        "default_season_grid_display_size",
        0,
    )

    fun seasonDownloadOverlay() = preferenceStore.getBoolean(
        "default_season_download_overlay",
        false,
    )

    fun seasonUnseenOverlay() = preferenceStore.getBoolean(
        "default_season_unseen_overlay",
        true,
    )

    fun seasonLocalOverlay() = preferenceStore.getBoolean(
        "default_season_local_overlay",
        true,
    )

    fun seasonLangOverlay() = preferenceStore.getBoolean(
        "default_season_lang_overlay",
        false,
    )

    fun seasonContinueOverlay() = preferenceStore.getBoolean(
        "default_season_continue_overlay",
        true,
    )

    fun seasonDisplayMode() = preferenceStore.getLong(
        "default_season_display_mode",
        Anime.SEASON_DISPLAY_MODE_SOURCE,
    )

    fun setSeasonSettingsDefault(anime: Anime) {
        filterSeasonByDownload().set(anime.seasonUnseenFilterRaw)
        filterSeasonByUnseen().set(anime.seasonUnseenFilterRaw)
        filterSeasonByStarted().set(anime.seasonStartedFilterRaw)
        filterSeasonByCompleted().set(anime.seasonCompletedFilterRaw)
        filterSeasonByBookmarked().set(anime.seasonBookmarkedFilterRaw)
        filterSeasonByFillermarked().set(anime.seasonFillermarkedFilterRaw)
        sortSeasonBySourceOrNumber().set(anime.seasonSorting)
        sortSeasonByAscendingOrDescending().set(
            if (anime.seasonSortDescending()) Anime.SEASON_SORT_DESC else Anime.SEASON_SORT_ASC,
        )
        seasonDisplayGridMode().set(SeasonDisplayMode.toLong(anime.seasonDisplayGridMode))
        seasonDisplayGridSize().set(anime.seasonDisplayGridSize)
        seasonDownloadOverlay().set(anime.seasonDownloadedOverlay)
        seasonUnseenOverlay().set(anime.seasonUnseenOverlay)
        seasonLocalOverlay().set(anime.seasonLocalOverlay)
        seasonLangOverlay().set(anime.seasonLangOverlay)
        seasonContinueOverlay().set(anime.seasonContinueOverlay)
        seasonDisplayMode().set(anime.seasonDisplayMode)
    }

    // Season behavior

    fun updateSeasonOnRefresh() =
        preferenceStore.getBoolean("pref_update_season_on_refresh", false)

    fun updateSeasonOnLibraryUpdate() =
        preferenceStore.getBoolean("pref_update_season_on_library_update", false)

    // region Item behavior

    fun swipeEpisodeStartAction() =
        preferenceStore.getEnum("pref_episode_swipe_end_action", EpisodeSwipeAction.ToggleSeen)

    fun swipeEpisodeEndAction() = preferenceStore.getEnum(
        "pref_episode_swipe_start_action",
        EpisodeSwipeAction.ToggleBookmark,
    )

    fun swipeChapterStartAction() =
        preferenceStore.getEnum("pref_chapter_swipe_end_action", ChapterSwipeAction.ToggleRead)

    fun swipeChapterEndAction() = preferenceStore.getEnum(
        "pref_chapter_swipe_start_action",
        ChapterSwipeAction.ToggleBookmark,
    )

    fun markDuplicateReadChapterAsRead() = preferenceStore.getStringSet("mark_duplicate_read_chapter_read", emptySet())

    fun markDuplicateSeenEpisodeAsSeen() = preferenceStore.getStringSet("mark_duplicate_seen_episode_seen", emptySet())

    // endregion

    enum class EpisodeSwipeAction {
        ToggleSeen,
        ToggleBookmark,
        ToggleFillermark,
        Download,
        Disabled,
    }

    enum class ChapterSwipeAction {
        ToggleRead,
        ToggleBookmark,
        Download,
        Disabled,
    }

    companion object {
        const val DEVICE_ONLY_ON_WIFI = "wifi"
        const val DEVICE_NETWORK_NOT_METERED = "network_not_metered"
        const val DEVICE_CHARGING = "ac"

        const val ENTRY_NON_COMPLETED = "manga_ongoing"
        const val ENTRY_HAS_UNVIEWED = "manga_fully_read"
        const val ENTRY_NON_VIEWED = "manga_started"
        const val ENTRY_OUTSIDE_RELEASE_PERIOD = "manga_outside_release_period"

        const val MARK_DUPLICATE_CHAPTER_READ_NEW = "new"
        const val MARK_DUPLICATE_CHAPTER_READ_EXISTING = "existing"
        const val MARK_DUPLICATE_EPISODE_SEEN_NEW = "new_episode"
        const val MARK_DUPLICATE_EPISODE_SEEN_EXISTING = "existing_episode"

        const val DEFAULT_MANGA_CATEGORY_PREF_KEY = "default_category"
        const val DEFAULT_ANIME_CATEGORY_PREF_KEY = "default_anime_category"
        private const val LIBRARY_UPDATE_MANGA_CATEGORIES_PREF_KEY = "library_update_categories"
        private const val LIBRARY_UPDATE_ANIME_CATEGORIES_PREF_KEY = "animelib_update_categories"
        private const val LIBRARY_UPDATE_MANGA_CATEGORIES_EXCLUDE_PREF_KEY = "library_update_categories_exclude"
        private const val LIBRARY_UPDATE_ANIME_CATEGORIES_EXCLUDE_PREF_KEY = "animelib_update_categories_exclude"
        val categoryPreferenceKeys = setOf(
            DEFAULT_MANGA_CATEGORY_PREF_KEY,
            DEFAULT_ANIME_CATEGORY_PREF_KEY,
            LIBRARY_UPDATE_MANGA_CATEGORIES_PREF_KEY,
            LIBRARY_UPDATE_ANIME_CATEGORIES_PREF_KEY,
            LIBRARY_UPDATE_MANGA_CATEGORIES_EXCLUDE_PREF_KEY,
            LIBRARY_UPDATE_ANIME_CATEGORIES_EXCLUDE_PREF_KEY,
        )
    }
}
