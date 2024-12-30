package eu.kanade.tachiyomi.ui.player.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR

class AudioPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun preferredAudioLanguages() = preferenceStore.getString("pref_audio_lang", "")
    fun enablePitchCorrection() = preferenceStore.getBoolean("pref_audio_pitch_correction", true)
    fun audioChannels() = preferenceStore.getEnum("pref_audio_config", AudioChannels.AutoSafe)
    fun volumeBoostCap() = preferenceStore.getInt("pref_audio_volume_boost_cap", 30)

    // Non-preferences

    fun audioDelay() = preferenceStore.getInt("pref_audio_delay", 0)
}

enum class AudioChannels(val titleRes: StringResource, val property: String, val value: String) {
    Auto(MR.strings.pref_player_audio_channels_auto, "audio-channels", "auto-safe"),
    AutoSafe(MR.strings.pref_player_audio_channels_auto_safe, "audio-channels", "auto"),
    Mono(MR.strings.pref_player_audio_channels_mono, "audio-channels", "mono"),
    Stereo(MR.strings.pref_player_audio_channels_stereo, "audio-channels", "stereo"),
    ReverseStereo(MR.strings.pref_player_audio_channels_reverse_stereo, "af", "pan=[stereo|c0=c1|c1=c0]"),
}
