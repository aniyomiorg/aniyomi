package eu.kanade.domain.source.service

import eu.kanade.domain.library.model.LibraryDisplayMode
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.core.preference.getEnum
import eu.kanade.tachiyomi.util.system.LocaleHelper

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
}
