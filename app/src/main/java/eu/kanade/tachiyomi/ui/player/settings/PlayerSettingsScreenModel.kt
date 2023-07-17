package eu.kanade.tachiyomi.ui.player.settings

import cafe.adriel.voyager.core.model.ScreenModel
import eu.kanade.tachiyomi.util.preference.toggle
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PlayerSettingsScreenModel(
    val preferences: PlayerPreferences = Injekt.get(),
) : ScreenModel {

    fun togglePreference(preference: (PlayerPreferences) -> Preference<Boolean>) {
        preference(preferences).toggle()
    }
}
