package eu.kanade.domain.source.service

import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum
import tachiyomi.domain.library.model.LibraryDisplayMode

class SourcePreferences(
    private val preferenceStore: PreferenceStore,
) {

    // Common options

    fun sourceDisplayMode() = preferenceStore.getObject("pref_display_mode_catalogue", LibraryDisplayMode.default, LibraryDisplayMode.Serializer::serialize, LibraryDisplayMode.Serializer::deserialize)

    fun enabledLanguages() = preferenceStore.getStringSet("source_languages", LocaleHelper.getDefaultEnabledLanguages())

    fun showNsfwSource() = preferenceStore.getBoolean("show_nsfw_source", true)

    fun migrationSortingMode() = preferenceStore.getEnum("pref_migration_sorting", SetMigrateSorting.Mode.ALPHABETICAL)

    fun migrationSortingDirection() = preferenceStore.getEnum("pref_migration_direction", SetMigrateSorting.Direction.ASCENDING)

    fun trustedSignatures() = preferenceStore.getStringSet("trusted_signatures", emptySet())

    // Mixture Sources

    fun disabledAnimeSources() = preferenceStore.getStringSet("hidden_anime_catalogues", emptySet())
    fun disabledMangaSources() = preferenceStore.getStringSet("hidden_catalogues", emptySet())

    fun pinnedAnimeSources() = preferenceStore.getStringSet("pinned_anime_catalogues", emptySet())
    fun pinnedMangaSources() = preferenceStore.getStringSet("pinned_catalogues", emptySet())

    fun lastUsedAnimeSource() = preferenceStore.getLong("last_anime_catalogue_source", -1)
    fun lastUsedMangaSource() = preferenceStore.getLong("last_catalogue_source", -1)

    fun animeExtensionUpdatesCount() = preferenceStore.getInt("animeext_updates_count", 0)
    fun mangaExtensionUpdatesCount() = preferenceStore.getInt("ext_updates_count", 0)

    fun searchPinnedAnimeSourcesOnly() = preferenceStore.getBoolean("search_pinned_anime_sources_only", false)
    fun searchPinnedMangaSourcesOnly() = preferenceStore.getBoolean("search_pinned_sources_only", false)

    fun hideInAnimeLibraryItems() = preferenceStore.getBoolean("browse_hide_in_anime_library_items", false)

    fun hideInMangaLibraryItems() = preferenceStore.getBoolean("browse_hide_in_library_items", false)

    // SY -->

    // fun enableSourceBlacklist() = preferenceStore.getBoolean("eh_enable_source_blacklist", true)

    // fun sourcesTabCategories() = preferenceStore.getStringSet("sources_tab_categories", mutableSetOf())

    // fun sourcesTabCategoriesFilter() = preferenceStore.getBoolean("sources_tab_categories_filter", false)

    // fun sourcesTabSourcesInCategories() = preferenceStore.getStringSet("sources_tab_source_categories", mutableSetOf())

    fun dataSaver() = preferenceStore.getEnum("data_saver", DataSaver.NONE)

    fun dataSaverIgnoreJpeg() = preferenceStore.getBoolean("ignore_jpeg", false)

    fun dataSaverIgnoreGif() = preferenceStore.getBoolean("ignore_gif", true)

    fun dataSaverImageQuality() = preferenceStore.getInt("data_saver_image_quality", 80)

    fun dataSaverImageFormatJpeg() = preferenceStore.getBoolean("data_saver_image_format_jpeg", false)

    fun dataSaverServer() = preferenceStore.getString("data_saver_server", "")

    fun dataSaverColorBW() = preferenceStore.getBoolean("data_saver_color_bw", false)

    fun dataSaverExcludedSources() = preferenceStore.getStringSet("data_saver_excluded", emptySet())

    fun dataSaverDownloader() = preferenceStore.getBoolean("data_saver_downloader", true)

    enum class DataSaver {
        NONE,
        BANDWIDTH_HERO,
        WSRV_NL,
        RESMUSH_IT,
    }
    // SY <--
}
