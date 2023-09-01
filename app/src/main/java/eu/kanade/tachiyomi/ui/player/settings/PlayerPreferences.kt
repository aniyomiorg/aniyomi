package eu.kanade.tachiyomi.ui.player.settings

import eu.kanade.tachiyomi.ui.player.viewer.AspectState
import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import tachiyomi.core.preference.PreferenceStore

class PlayerPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun preserveWatchingPosition() = preferenceStore.getBoolean("pref_preserve_watching_position", false)

    fun enablePip() = preferenceStore.getBoolean("pref_enable_pip", true)

    fun pipEpisodeToasts() = preferenceStore.getBoolean("pref_pip_episode_toasts", true)

    fun pipOnExit() = preferenceStore.getBoolean("pref_pip_on_exit", false)

    fun rememberPlayerBrightness() = preferenceStore.getBoolean("pref_remember_brightness", false)

    fun playerBrightnessValue() = preferenceStore.getFloat("player_brightness_value", -1.0F)

    fun rememberPlayerVolume() = preferenceStore.getBoolean("pref_remember_volume", false)

    fun playerVolumeValue() = preferenceStore.getFloat("player_volume_value", -1.0F)

    fun autoplayEnabled() = preferenceStore.getBoolean("pref_auto_play_enabled", false)

    fun invertedPlaybackTxt() = preferenceStore.getBoolean("pref_invert_playback_txt", false)

    fun invertedDurationTxt() = preferenceStore.getBoolean("pref_invert_duration_txt", false)

    fun mpvConf() = preferenceStore.getString("pref_mpv_conf", "")

    fun defaultPlayerOrientationType() = preferenceStore.getInt("pref_default_player_orientation_type_key", 10)

    fun adjustOrientationVideoDimensions() = preferenceStore.getBoolean("pref_adjust_orientation_video_dimensions", true)

    fun defaultPlayerOrientationLandscape() = preferenceStore.getInt("pref_default_player_orientation_landscape_key", 6)

    fun defaultPlayerOrientationPortrait() = preferenceStore.getInt("pref_default_player_orientation_portrait_key", 7)

    fun playerSpeed() = preferenceStore.getFloat("pref_player_speed", 1F)

    fun playerSmoothSeek() = preferenceStore.getBoolean("pref_player_smooth_seek", false)

    fun mediaChapterSeek() = preferenceStore.getBoolean("pref_media_control_chapter_seeking", false)

    fun playerViewMode() = preferenceStore.getInt("pref_player_view_mode", AspectState.FIT.index)

    fun playerFullscreen() = preferenceStore.getBoolean("player_fullscreen", true)

    fun hideControls() = preferenceStore.getBoolean("player_hide_controls", false)

    fun screenshotSubtitles() = preferenceStore.getBoolean("pref_screenshot_subtitles", false)

    fun gestureVolumeBrightness() = preferenceStore.getBoolean("pref_gesture_volume_brightness", true)

    fun gestureHorizontalSeek() = preferenceStore.getBoolean("pref_gesture_horizontal_seek", true)

    fun alwaysUseExternalPlayer() = preferenceStore.getBoolean("pref_always_use_external_player", false)

    fun externalPlayerPreference() = preferenceStore.getString("external_player_preference", "")

    fun progressPreference() = preferenceStore.getFloat("pref_progress_preference", 0.85F)

    fun defaultIntroLength() = preferenceStore.getInt("pref_default_intro_length", 85)

    fun skipLengthPreference() = preferenceStore.getInt("pref_skip_length_preference", 10)

    fun aniSkipEnabled() = preferenceStore.getBoolean("pref_enable_ani_skip", false)

    fun autoSkipAniSkip() = preferenceStore.getBoolean("pref_enable_auto_skip_ani_skip", false)

    fun waitingTimeAniSkip() = preferenceStore.getInt("pref_waiting_time_aniskip", 5)

    fun enableNetflixStyleAniSkip() = preferenceStore.getBoolean("pref_enable_netflixStyle_aniskip", false)

    fun standardHwDec() = preferenceStore.getString("pref_hwdec", HwDecState.defaultHwDec.mpvValue)

    fun deband() = preferenceStore.getInt("pref_deband", 0)
}
