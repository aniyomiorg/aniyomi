package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.util.preference.asState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object AdvancedPlayerSettingsScreen : SearchableSettings {
    @Composable
    override fun getTitleRes() = R.string.pref_category_player_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
        val context = LocalContext.current
        val mpvConf = playerPreferences.mpvConf()
        val scope = rememberCoroutineScope()

        return listOf(
            Preference.PreferenceItem.MultiLineEditTextPreference(
                pref = mpvConf,
                title = context.getString(R.string.pref_mpv_conf),
                subtitle = mpvConf.asState(scope).value
                    .lines().take(2)
                    .joinToString(
                        separator = "\n",
                        postfix = if (mpvConf.asState(scope).value.lines().size > 2) "\n..." else "",
                    ),

            ),
            Preference.PreferenceItem.ListPreference(
                title = context.getString(R.string.pref_debanding_title),
                pref = playerPreferences.deband(),
                entries = mapOf(
                    0 to context.getString(R.string.pref_debanding_disabled),
                    1 to context.getString(R.string.pref_debanding_cpu),
                    2 to context.getString(R.string.pref_debanding_gpu),
                    3 to "YUV420P",
                ),
            ),
        )
    }
}
