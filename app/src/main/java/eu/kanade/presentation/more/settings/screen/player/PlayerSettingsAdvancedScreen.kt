package eu.kanade.presentation.more.settings.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.player.settings.AdvancedPlayerPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object PlayerSettingsAdvancedScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_player_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val advancedPlayerPreferences = remember { Injekt.get<AdvancedPlayerPreferences>() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val enableUserFiles = advancedPlayerPreferences.mpvUserFiles()
        val mpvConf = advancedPlayerPreferences.mpvConf()
        val mpvInput = advancedPlayerPreferences.mpvInput()

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = enableUserFiles,
                title = stringResource(MR.strings.pref_mpv_user_files),
                subtitle = stringResource(MR.strings.pref_mpv_user_files_summary),
            ),
            Preference.PreferenceItem.MPVConfPreference(
                preference = mpvConf,
                scope = scope,
                context = context,
                fileName = "mpv.conf",
                title = stringResource(MR.strings.pref_mpv_conf),
            ),
            Preference.PreferenceItem.MPVConfPreference(
                preference = mpvInput,
                scope = scope,
                context = context,
                fileName = "input.conf",
                title = stringResource(MR.strings.pref_mpv_input),
            ),
        )
    }
}
