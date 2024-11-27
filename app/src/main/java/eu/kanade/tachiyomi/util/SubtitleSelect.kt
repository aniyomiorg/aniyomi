package eu.kanade.tachiyomi.util

import androidx.core.os.LocaleListCompat
import eu.kanade.tachiyomi.ui.player.PlayerViewModel.VideoTrack
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import java.util.Locale

class SubtitleSelect(private val subtitlePreferences: SubtitlePreferences) {

    fun getPreferredSubtitleIndex(tracks: List<VideoTrack>): VideoTrack? {
        val prefLangs = subtitlePreferences.preferredSubLanguages().get().split(",")
            .filter { it.isNotEmpty() }
        val whitelist = subtitlePreferences.subtitleWhitelist().get().split(",")
            .filter { it.isNotEmpty() }
        val blacklist = subtitlePreferences.subtitleBlacklist().get().split(",")
            .filter { it.isNotEmpty() }

        val locales = prefLangs.map(::Locale).ifEmpty {
            listOf(LocaleListCompat.getDefault()[0]!!)
        }

        val chosenLocale = locales.firstOrNull { locale ->
            tracks.any { t -> containsLang(t, locale) }
        } ?: return null

        val filtered = tracks.withIndex()
            .filterNot { (_, track) ->
                blacklist.any { track.name.contains(it, true) }
            }
            .filter { (_, track) ->
                containsLang(track, chosenLocale)
            }

        return filtered.firstOrNull { (_, track) ->
            whitelist.any { track.name.contains(it, true) }
        }?.value ?: filtered.getOrNull(0)?.value
    }

    private fun containsLang(track: VideoTrack, locale: Locale): Boolean {
        val localName = locale.getDisplayName(locale)
        val englishName = locale.getDisplayName(Locale.ENGLISH).substringBefore(" (")
        val langRegex = Regex("""\b${locale.isO3Language}|${locale.language}\b""", RegexOption.IGNORE_CASE)

        return track.name.contains(localName, true) ||
            track.name.contains(englishName, true) ||
            track.language?.let { langRegex.find(it) != null } == true
    }
}
