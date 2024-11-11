package eu.kanade.tachiyomi.util

import androidx.core.os.LocaleListCompat
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import java.util.Locale

class SubtitleSelect(private val subtitlePreferences: SubtitlePreferences) {

    fun getPreferredSubtitleIndex(tracks: List<Track>): Int? {
        val prefLangs = subtitlePreferences.preferredSubLanguages().get().split(",")
        val whitelist = subtitlePreferences.subtitleWhitelist().get().split(",")
        val blacklist = subtitlePreferences.subtitleBlacklist().get().split(",")

        val locales = prefLangs.map(::Locale).ifEmpty {
            listOf(LocaleListCompat.getDefault()[0]!!)
        }
        val chosenLocale = locales.firstOrNull { locale ->
            tracks.any { t -> containsLang(t.lang, locale) }
        } ?: return null

        val filtered = tracks.withIndex()
            .filterNot { (_, track) ->
                blacklist.any { track.lang.contains(it, true) }
            }
            .filter { (_, track) ->
                containsLang(track.lang, chosenLocale)
            }

        return filtered.firstOrNull { (_, track) ->
            whitelist.any { track.lang.contains(it, true) }
        }?.index ?: filtered.getOrNull(0)?.index
    }

    private fun containsLang(title: String, locale: Locale): Boolean {
        val localName = locale.getDisplayName(locale)
        val englishName = locale.getDisplayName(Locale.ENGLISH).substringBefore(" (")
        val langRegex = Regex("""\b${locale.isO3Language}|${locale.language}\b""", RegexOption.IGNORE_CASE)

        return title.contains(localName, true) ||
            title.contains(englishName, true) ||
            langRegex.find(title) != null
    }
}
