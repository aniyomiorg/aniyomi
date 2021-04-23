package eu.kanade.tachiyomi.source

import androidx.preference.PreferenceScreen

interface ConfigurableAnimeSource : AnimeSource {

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
