package eu.kanade.tachiyomi.util

import androidx.core.os.LocaleListCompat
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class SubtitleSelect(private val playerPreferences: PlayerPreferences) {

    private val json: Json by injectLazy()

    fun getPreferredSubtitleIndex(tracks: List<Track>): Int? {
        val config = try {
            json.decodeFromString<SubConfig>(playerPreferences.subSelectConf().get())
        } catch (e: SerializationException) {
            logcat(LogPriority.WARN, e) { "Invalid subtitle select configuration" }
            SubConfig()
        }

        val locale = if (config.lang == null) {
            LocaleListCompat.getDefault()[0]!!
        } else {
            Locale(config.lang)
        }
        val langNames = listOf(
            locale.getDisplayName(locale),
            locale.getDisplayName(Locale.ENGLISH).substringBefore(" ("),
        )
        val langRegex = Regex("""\b${locale.getISO3Language()}\b""")

        val filtered = tracks.withIndex()
            .filterNot { (_, track) ->
                config.blacklist.any { track.lang.contains(it, true) }
            }
            .filter { (_, track) ->
                langNames.any { track.lang.contains(it, true) } ||
                    langRegex.find(track.lang) != null
            }

        return filtered.firstOrNull { (_, track) ->
            config.whitelist.any { track.lang.contains(it, true) }
        }?.index ?: filtered.getOrNull(0)?.index
    }

    @Serializable
    data class SubConfig(
        val lang: String? = null,
        val blacklist: List<String> = emptyList(),
        val whitelist: List<String> = emptyList(),
    )
}
