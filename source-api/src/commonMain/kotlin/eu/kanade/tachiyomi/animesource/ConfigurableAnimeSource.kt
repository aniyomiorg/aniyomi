package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.PreferenceScreen

interface ConfigurableAnimeSource : AnimeSource {

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
