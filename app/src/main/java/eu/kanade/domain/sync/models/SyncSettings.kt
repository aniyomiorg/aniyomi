package eu.kanade.domain.sync.models

data class SyncSettings(
    val libraryEntries: Boolean = true,
    val animelibEntries: Boolean = true,
    val categories: Boolean = true,
    val animeCategories: Boolean = true,
    val chapters: Boolean = true,
    val episodes: Boolean = true,
    val tracking: Boolean = true,
    val animeTracking: Boolean = true,
    val history: Boolean = true,
    val animeHistory: Boolean = true,
    val appSettings: Boolean = true,
    val sourceSettings: Boolean = true,
    val privateSettings: Boolean = false,
)
