package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsPlayerController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_player

        preferenceCategory {
            titleRes = R.string.pref_category_display

            /*intListPreference {
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
            }*/
            listPreference {
                key = Keys.progressPreference
                titleRes = R.string.pref_progress_mark_as_seen

                entriesRes = arrayOf(
                    R.string.pref_progress_100,
                    R.string.pref_progress_95,
                    R.string.pref_progress_90,
                    R.string.pref_progress_85,
                    R.string.pref_progress_80,
                    R.string.pref_progress_75,
                    R.string.pref_progress_70
                )
                entryValues = arrayOf(
                    "1.00F",
                    "0.95F",
                    "0.90F",
                    "0.85F",
                    "0.80F",
                    "0.75F",
                    "0.70F"
                )
                defaultValue = "0.85F"

                summary = "%s"
            }
            switchPreference {
                key = Keys.alwaysUseExternalPlayer
                titleRes = R.string.pref_always_use_external_player
                defaultValue = false
            }
            switchPreference {
                key = Keys.useExternalDownloader
                titleRes = R.string.pref_use_external_downloader
                defaultValue = false
            }
        }
    }
}
