package eu.kanade.presentation.more.settings.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.player.settings.AudioChannels
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object PlayerSettingsAudioScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_player_audio

    @Composable
    override fun getPreferences(): List<Preference> {
        val audioPreferences = remember { Injekt.get<AudioPreferences>() }

        val prefLangs = audioPreferences.preferredAudioLanguages()
        val pitchCorrection = audioPreferences.enablePitchCorrection()
        val audioChannels = audioPreferences.audioChannels()
        val boostCapPref = audioPreferences.volumeBoostCap()
        val boostCap by boostCapPref.collectAsState()

        return listOf(
            Preference.PreferenceItem.EditTextInfoPreference(
                preference = prefLangs,
                dialogSubtitle = stringResource(MR.strings.pref_player_audio_lang_info),
                title = stringResource(MR.strings.pref_player_audio_lang),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = pitchCorrection,
                title = stringResource(MR.strings.pref_player_audio_pitch_correction),
                subtitle = stringResource(MR.strings.pref_player_audio_pitch_correction_summary),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = audioChannels,
                entries = AudioChannels.entries.associateWith {
                    stringResource(it.titleRes)
                }.toImmutableMap(),
                title = stringResource(MR.strings.pref_player_audio_channels),
            ),
            Preference.PreferenceItem.SliderPreference(
                value = boostCap,
                valueRange = 0..200,
                title = stringResource(MR.strings.pref_player_audio_boost_cap),
                subtitle = boostCap.toString(),
                onValueChanged = {
                    boostCapPref.set(it)
                    true
                },
            ),
        )
    }
}
