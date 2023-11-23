package eu.kanade.tachiyomi.animesource

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.PreferenceScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface ConfigurableAnimeSource : AnimeSource {

    /**
     * Gets instance of [SharedPreferences] scoped to the specific source.
     *
     * @since extensions-lib 1.5
     */
    fun getPreferences(): SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", Context.MODE_PRIVATE)

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
