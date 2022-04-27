package eu.kanade.tachiyomi.ui.setting

import android.content.pm.PackageManager
import android.os.Build
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsPlayerController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_player

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
                R.string.pref_progress_70,
            )
            entryValues = arrayOf(
                "1.00F",
                "0.95F",
                "0.90F",
                "0.85F",
                "0.80F",
                "0.75F",
                "0.70F",
            )
            defaultValue = "0.85F"

            summary = "%s"
        }

        listPreference {
            key = Keys.skipLengthPreference
            titleRes = R.string.pref_skip_length

            entriesRes = arrayOf(
                R.string.pref_skip_30,
                R.string.pref_skip_20,
                R.string.pref_skip_10,
                R.string.pref_skip_5,
            )
            entryValues = arrayOf(
                "30",
                "20",
                "10",
                "5",
            )
            defaultValue = "10"

            summary = "%s"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchPreference {
                key = "player_fullscreen"
                titleRes = R.string.pref_player_fullscreen
                defaultValue = true
            }
        }

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            switchPreference {
                key = Keys.pipEpisodeToasts
                titleRes = R.string.pref_pip_episode_toasts
                defaultValue = true
            }
        }

        switchPreference {
            key = Keys.alwaysUseExternalPlayer
            titleRes = R.string.pref_always_use_external_player
            defaultValue = false
        }

        listPreference {
            key = Keys.externalPlayerPreference
            titleRes = R.string.pref_external_player_preference

            val pm = context.packageManager
            val installedPackages = pm.getInstalledPackages(0)
            val supportedPlayers = installedPackages.filter {
                when (it.packageName) {
                    "is.xyz.mpv" -> true
                    "com.mxtech.videoplayer" -> true
                    "com.mxtech.videoplayer.ad" -> true
                    "com.mxtech.videoplayer.pro" -> true
                    "org.videolan.vlc" -> true
                    "com.husudosu.mpvremote" -> true
                    else -> false
                }
            }
            val packageNames = supportedPlayers.map { it.packageName }
            val packageNamesReadable = supportedPlayers
                .map { pm.getApplicationLabel(it.applicationInfo).toString() }

            entries = arrayOf("None") + packageNamesReadable.toTypedArray()
            entryValues = arrayOf("") + packageNames.toTypedArray()
            defaultValue = ""

            summary = "%s"
        }
    }
}
