package eu.kanade.tachiyomi.data.backup.legacy.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Json values
 */
object Backup {
    const val CURRENT_VERSION = 2
    const val MANGA = "manga"
    const val MANGAS = "mangas"
    const val ANIMES = "animes"
    const val TRACK = "track"
    const val ANIMETRACK = "animetrack"
    const val CHAPTERS = "chapters"
    const val EPISODES = "episodes"
    const val CATEGORIES = "categories"
    const val ANIMECATEGORIES = "animecategories"
    const val EXTENSIONS = "extensions"
    const val ANIMEEXTENSIONS = "animeextensions"
    const val HISTORY = "history"
    const val VERSION = "version"

    fun getDefaultFilename(): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        return "tachiyomi_$date.json"
    }
}
