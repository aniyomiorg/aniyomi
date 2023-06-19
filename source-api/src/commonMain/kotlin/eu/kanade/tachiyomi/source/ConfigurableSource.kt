package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.PreferenceScreen

interface ConfigurableSource : MangaSource {

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
