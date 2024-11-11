package eu.kanade.tachiyomi.ui.player.settings

import eu.kanade.tachiyomi.ui.player.viewer.AspectState
import eu.kanade.tachiyomi.ui.player.viewer.InvertedPlayback
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class PlayerPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun preserveWatchingPosition() = preferenceStore.getBoolean(
        "pref_preserve_watching_position",
        false,
    )
    fun progressPreference() = preferenceStore.getFloat("pref_progress_preference", 0.85F)

    fun playerFullscreen() = preferenceStore.getBoolean("player_fullscreen", true)
    fun hideControls() = preferenceStore.getBoolean("player_hide_controls", false)

    // Volume and brightness

    fun gestureVolumeBrightness() = preferenceStore.getBoolean(
        "pref_gesture_volume_brightness",
        true,
    )
    fun rememberPlayerBrightness() = preferenceStore.getBoolean("pref_remember_brightness", false)
    fun playerBrightnessValue() = preferenceStore.getFloat("player_brightness_value", -1.0F)
    fun rememberPlayerVolume() = preferenceStore.getBoolean("pref_remember_volume", false)
    fun playerVolumeValue() = preferenceStore.getFloat("player_volume_value", -1.0F)

    // Orientation

    fun defaultPlayerOrientationType() = preferenceStore.getInt(
        "pref_default_player_orientation_type_key",
        10,
    )
    fun adjustOrientationVideoDimensions() = preferenceStore.getBoolean(
        "pref_adjust_orientation_video_dimensions",
        true,
    )
    fun defaultPlayerOrientationPortrait() = preferenceStore.getInt(
        "pref_default_player_orientation_portrait_key",
        7,
    )
    fun defaultPlayerOrientationLandscape() = preferenceStore.getInt(
        "pref_default_player_orientation_landscape_key",
        6,
    )

    // PiP

    fun enablePip() = preferenceStore.getBoolean("pref_enable_pip", true)
    fun pipEpisodeToasts() = preferenceStore.getBoolean("pref_pip_episode_toasts", true)
    fun pipOnExit() = preferenceStore.getBoolean("pref_pip_on_exit", false)
    fun pipReplaceWithPrevious() = preferenceStore.getBoolean("pip_replace_with_previous", false)

    // External player

    fun alwaysUseExternalPlayer() = preferenceStore.getBoolean(
        "pref_always_use_external_player",
        false,
    )
    fun externalPlayerPreference() = preferenceStore.getString("external_player_preference", "")

    // Non-preferences

    fun autoplayEnabled() = preferenceStore.getBoolean("pref_auto_play_enabled", false)

    fun invertedPlayback() = preferenceStore.getEnum("pref_inverted_playback", InvertedPlayback.NONE)

    fun playerSpeed() = preferenceStore.getFloat("pref_player_speed", 1F)

    fun aspectState() = preferenceStore.getEnum("pref_player_aspect_state", AspectState.FIT)
    fun brightnessFilter() = preferenceStore.getInt("pref_player_filter_brightness")
    fun saturationFilter() = preferenceStore.getInt("pref_player_filter_saturation")
    fun contrastFilter() = preferenceStore.getInt("pref_player_filter_contrast")
    fun gammaFilter() = preferenceStore.getInt("pref_player_filter_gamma")
    fun hueFilter() = preferenceStore.getInt("pref_player_filter_hue")
}
