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

        val locales = config.lang.map(::Locale).ifEmpty {
            listOf(LocaleListCompat.getDefault()[0]!!)
        }
        val chosenLocale = locales.firstOrNull { locale ->
            tracks.any { t -> containsLang(t.lang, locale) }
        } ?: return null

        val filtered = tracks.withIndex()
            .filterNot { (_, track) ->
                config.blacklist.any { track.lang.contains(it, true) }
            }
            .filter { (_, track) ->
                containsLang(track.lang, chosenLocale)
            }

        return filtered.firstOrNull { (_, track) ->
            config.whitelist.any { track.lang.contains(it, true) }
        }?.index ?: filtered.getOrNull(0)?.index
    }

    private fun containsLang(title: String, locale: Locale): Boolean {
        val localName = locale.getDisplayName(locale)
        val englishName = locale.getDisplayName(Locale.ENGLISH).substringBefore(" (")
        val langRegex = Regex("""\b${locale.getISO3Language()}\b""", RegexOption.IGNORE_CASE)

        return title.contains(localName) || title.contains(englishName) || langRegex.find(title) != null
    }

    @Serializable
    data class SubConfig(
        val lang: List<String> = emptyList(),
        val blacklist: List<String> = emptyList(),
        val whitelist: List<String> = emptyList(),
    )
}
