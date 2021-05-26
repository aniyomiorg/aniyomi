package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsPlayerController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_player

        preferenceCategory {
            titleRes = R.string.pref_category_display

            intListPreference {
                key = Keys.defaultPlayerOrientationType
                titleRes = R.string.pref_rotation_type
                entriesRes = arrayOf(
                    R.string.rotation_free,
                    R.string.rotation_portrait,
                    R.string.rotation_landscape,
                    R.string.rotation_force_portrait,
                    R.string.rotation_force_landscape,
                )
                entryValues = OrientationType.values().drop(1)
                    .map { value -> "${value.flagValue}" }.toTypedArray()
                defaultValue = "${OrientationType.FREE.flagValue}"
                summary = "%s"
            }
            switchPreference {
                key = Keys.fullscreen
                titleRes = R.string.pref_fullscreen
                defaultValue = true
            }
            switchPreference {
                key = Keys.alwaysUseExternalPlayer
                titleRes = R.string.pref_always_use_external_player
                defaultValue = false
            }
        }
    }
}
