package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
import android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
import android.view.LayoutInflater
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PrefSkipIntroLengthBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.editTextPreference
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
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

        switchPreference {
            key = Keys.preserveWatchingPosition
            titleRes = R.string.pref_preserve_watching_position
            defaultValue = false
        }

        preferenceCategory {
            titleRes = R.string.pref_category_player_orientation

            listPreference {
                key = Keys.defaultPlayerOrientationType
                titleRes = R.string.pref_default_player_orientation

                entriesRes = arrayOf(
                    R.string.rotation_free,
                    R.string.rotation_portrait,
                    R.string.rotation_reverse_portrait,
                    R.string.rotation_landscape,
                    R.string.rotation_reverse_landscape,
                    R.string.rotation_sensor_portrait,
                    R.string.rotation_sensor_landscape,
                )
                entryValues = arrayOf(
                    "${ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR}",
                    "${ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}",
                    "${ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT}",
                    "${ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}",
                    "${ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE}",
                    "${ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT}",
                    "${ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE}",
                )
                defaultValue = "${ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR}"

                summary = "%s"
            }

            switchPreference {
                key = Keys.adjustOrientationVideoDimensions
                titleRes = R.string.pref_adjust_orientation_video_dimensions
                defaultValue = true
            }

            listPreference {
                key = Keys.defaultPlayerOrientationPortrait
                titleRes = R.string.pref_default_portrait_orientation

                entriesRes = arrayOf(
                    R.string.rotation_portrait,
                    R.string.rotation_reverse_portrait,
                    R.string.rotation_sensor_portrait,
                )
                entryValues = arrayOf(
                    "${ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}",
                    "${ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT}",
                    "${ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT}",
                )
                defaultValue = "${ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT}"

                summary = "%s"
            }

            listPreference {
                key = Keys.defaultPlayerOrientationLandscape
                titleRes = R.string.pref_default_landscape_orientation

                entriesRes = arrayOf(
                    R.string.rotation_landscape,
                    R.string.rotation_reverse_landscape,
                    R.string.rotation_sensor_landscape,
                )
                entryValues = arrayOf(
                    "${ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}",
                    "${ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE}",
                    "${ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE}",
                )
                defaultValue = "${ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE}"

                summary = "%s"
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_internal_player

            preference {
                key = Keys.defaultIntroLength
                titleRes = R.string.pref_default_intro_length
                onClick {
                    SkipIntroLengthDialog().showDialog(router)
                }

                preferences.defaultIntroLength().asFlow()
                    .onEach { summary = it.toString() }
                    .launchIn(viewScope)
            }

            listPreference {
                key = Keys.skipLengthPreference
                titleRes = R.string.pref_skip_length

                entriesRes = arrayOf(
                    R.string.pref_skip_30,
                    R.string.pref_skip_20,
                    R.string.pref_skip_10,
                    R.string.pref_skip_5,
                    R.string.pref_skip_disable,
                )
                entryValues = arrayOf(
                    "30",
                    "20",
                    "10",
                    "5",
                    "0",
                )
                defaultValue = "10"

                summary = "%s"
            }

            switchPreference {
                key = Keys.playerSmoothSeek
                titleRes = R.string.pref_player_smooth_seek
                defaultValue = false
                summaryRes = R.string.pref_player_smooth_seek_summary
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchPreference {
                    key = "player_fullscreen"
                    titleRes = R.string.pref_player_fullscreen
                    defaultValue = true
                }
            }

            switchPreference {
                key = "player_hide_controls"
                titleRes = R.string.pref_player_hide_controls
                defaultValue = false
            }

            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                switchPreference {
                    key = Keys.pipEpisodeToasts
                    titleRes = R.string.pref_pip_episode_toasts
                    defaultValue = true
                }

                switchPreference {
                    key = Keys.pipOnExit
                    titleRes = R.string.pref_pip_on_exit
                    defaultValue = false
                }
            }

            editTextPreference {
                key = Keys.mpvConf
                titleRes = R.string.pref_mpv_conf
                setOnBindEditTextListener {
                    it.inputType =
                        TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_MULTI_LINE or TYPE_TEXT_FLAG_NO_SUGGESTIONS
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_external_player

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
}

class SkipIntroLengthDialog : DialogController() {

    private val preferences: PreferencesHelper = Injekt.get()

    private var defaultIntroLength = preferences.defaultIntroLength().get()

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val binding = PrefSkipIntroLengthBinding.inflate(LayoutInflater.from(activity!!))
        onViewCreated(binding)
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.pref_intro_length)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                preferences.defaultIntroLength().set(defaultIntroLength)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    fun onViewCreated(binding: PrefSkipIntroLengthBinding) {
        with(binding.skipIntroColumn) {
            value = defaultIntroLength

            setOnValueChangedListener { _, _, newValue ->
                defaultIntroLength = newValue
            }
        }
    }
}
