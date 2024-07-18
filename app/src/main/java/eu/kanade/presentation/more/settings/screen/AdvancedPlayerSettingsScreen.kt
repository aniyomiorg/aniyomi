package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.viewer.VideoDebanding
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object AdvancedPlayerSettingsScreen : SearchableSettings {
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_player_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val mpvConf = playerPreferences.mpvConf()
        val mpvInput = playerPreferences.mpvInput()
        val subSelectConf = playerPreferences.subSelectConf()

        return listOf(
            Preference.PreferenceItem.MPVConfPreference(
                pref = mpvConf,
                title = context.stringResource(MR.strings.pref_mpv_conf),
                fileName = "mpv.conf",
                scope = scope,
                context = context,
            ),
            Preference.PreferenceItem.MPVConfPreference(
                pref = mpvInput,
                title = context.stringResource(MR.strings.pref_mpv_input),
                fileName = "input.conf",
                scope = scope,
                context = context,
            ),
            Preference.PreferenceItem.MPVConfPreference(
                pref = subSelectConf,
                title = context.stringResource(MR.strings.pref_sub_select_conf),
                scope = scope,
                context = context,
            ),
            Preference.PreferenceItem.SwitchPreference(
                title = context.stringResource(MR.strings.pref_gpu_next_title),
                subtitle = context.stringResource(MR.strings.pref_gpu_next_subtitle),
                pref = playerPreferences.gpuNext(),
            ),
            Preference.PreferenceItem.ListPreference(
                title = context.stringResource(MR.strings.pref_debanding_title),
                pref = playerPreferences.videoDebanding(),
                entries = VideoDebanding.entries.associateWith {
                    context.stringResource(
                        it.stringRes,
                    )
                }.toImmutableMap(),
            ),
            Preference.PreferenceItem.SwitchPreference(
                title = context.stringResource(MR.strings.pref_mpv_scripts),
                subtitle = context.stringResource(MR.strings.pref_mpv_scripts_summary),
                pref = playerPreferences.mpvScripts(),
                onValueChanged = {
                    // Ask for external storage permission
                    if (it) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        }
                    }
                    true
                },
            ),
        )
    }
}
