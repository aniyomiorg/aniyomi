package eu.kanade.tachiyomi.ui.player.settings

import eu.kanade.tachiyomi.ui.player.viewer.AspectState
import eu.kanade.tachiyomi.ui.player.viewer.AudioChannels
import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import eu.kanade.tachiyomi.ui.player.viewer.InvertedPlayback
import eu.kanade.tachiyomi.ui.player.viewer.SingleActionGesture
import eu.kanade.tachiyomi.ui.player.viewer.VideoDebanding
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class PlayerPreferences(
    private val preferenceStore: PreferenceStore,
) {
    // ==== Internal player ====

    fun preserveWatchingPosition() = preferenceStore.getBoolean(
        "pref_preserve_watching_position",
        false,
    )
    fun progressPreference() = preferenceStore.getFloat("pref_progress_preference", 0.85F)

    fun playerFullscreen() = preferenceStore.getBoolean("player_fullscreen", true)
    fun hideControls() = preferenceStore.getBoolean("player_hide_controls", false)

    // Internal player - Volume and brightness

    fun gestureVolumeBrightness() = preferenceStore.getBoolean(
        "pref_gesture_volume_brightness",
        true,
    )
    fun rememberPlayerBrightness() = preferenceStore.getBoolean("pref_remember_brightness", false)
    fun playerBrightnessValue() = preferenceStore.getFloat("player_brightness_value", -1.0F)
    fun rememberPlayerVolume() = preferenceStore.getBoolean("pref_remember_volume", false)
    fun playerVolumeValue() = preferenceStore.getFloat("player_volume_value", -1.0F)

    // Internal player - Orientation

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

    // Internal player - PiP

    fun enablePip() = preferenceStore.getBoolean("pref_enable_pip", true)
    fun pipEpisodeToasts() = preferenceStore.getBoolean("pref_pip_episode_toasts", true)
    fun pipOnExit() = preferenceStore.getBoolean("pref_pip_on_exit", false)
    fun pipReplaceWithPrevious() = preferenceStore.getBoolean("pip_replace_with_previous", false)

    // Internal player - External player

    fun alwaysUseExternalPlayer() = preferenceStore.getBoolean(
        "pref_always_use_external_player",
        false,
    )
    fun externalPlayerPreference() = preferenceStore.getString("external_player_preference", "")

    // ==== Gestures ====
    // Gestures - Seeking

    fun skipLengthPreference() = preferenceStore.getInt("pref_skip_length_preference", 10)
    fun gestureHorizontalSeek() = preferenceStore.getBoolean("pref_gesture_horizontal_seek", true)
    fun defaultIntroLength() = preferenceStore.getInt("pref_default_intro_length", 85)
    fun playerSmoothSeek() = preferenceStore.getBoolean("pref_player_smooth_seek", false)
    fun mediaChapterSeek() = preferenceStore.getBoolean("pref_media_control_chapter_seeking", false)

    fun aniSkipEnabled() = preferenceStore.getBoolean("pref_enable_ani_skip", false)
    fun autoSkipAniSkip() = preferenceStore.getBoolean("pref_enable_auto_skip_ani_skip", false)
    fun enableNetflixStyleAniSkip() = preferenceStore.getBoolean(
        "pref_enable_netflixStyle_aniskip",
        false,
    )
    fun waitingTimeAniSkip() = preferenceStore.getInt("pref_waiting_time_aniskip", 5)

    // Gestures - Double tap

    fun leftDoubleTapGesture() = preferenceStore.getEnum("pref_left_double_tap", SingleActionGesture.Seek)
    fun centerDoubleTapGesture() = preferenceStore.getEnum("pref_center_double_tap", SingleActionGesture.PlayPause)
    fun rightDoubleTapGesture() = preferenceStore.getEnum("pref_right_double_tap", SingleActionGesture.Seek)

    // Gestures - Media controls

    fun mediaPreviousGesture() = preferenceStore.getEnum("pref_media_previous", SingleActionGesture.Switch)
    fun mediaPlayPauseGesture() = preferenceStore.getEnum("pref_media_playpause", SingleActionGesture.PlayPause)
    fun mediaNextGesture() = preferenceStore.getEnum("pref_media_next", SingleActionGesture.Switch)

    // ==== Decoder ====

    fun tryHWDecoding() = preferenceStore.getBoolean("pref_try_hwdec", true)
    fun gpuNext() = preferenceStore.getBoolean("pref_gpu_next", false)
    fun videoDebanding() = preferenceStore.getEnum("pref_video_debanding", VideoDebanding.NONE)
    fun useYUV420P() = preferenceStore.getBoolean("use_yuv420p", true)

    // ==== Subtitle ====

    fun preferredSubLanguages() = preferenceStore.getString("pref_subtitle_lang", "")
    fun subtitleWhitelist() = preferenceStore.getString("pref_subtitle_whitelist", "")
    fun subtitleBlacklist() = preferenceStore.getString("pref_subtitle_blacklist", "")

    // ==== Audio ====

    fun preferredAudioLanguages() = preferenceStore.getString("pref_audio_lang", "")
    fun enablePitchCorrection() = preferenceStore.getBoolean("pref_audio_pitch_correction", true)
    fun audioChannels() = preferenceStore.getEnum("pref_audio_config", AudioChannels.AutoSafe)
    fun volumeBoostCap() = preferenceStore.getInt("pref_audio_volume_boost_cap", 30)

    // ==== Advanced ====

    fun mpvScripts() = preferenceStore.getBoolean("mpv_scripts", false)
    fun mpvConf() = preferenceStore.getString("pref_mpv_conf", "")
    fun mpvInput() = preferenceStore.getString("pref_mpv_input", "")

    // ==== Non-preferences ====

    fun autoplayEnabled() = preferenceStore.getBoolean("pref_auto_play_enabled", false)

    fun invertedPlayback() = preferenceStore.getEnum("pref_inverted_playback", InvertedPlayback.NONE)

    fun subSelectConf() = preferenceStore.getString("pref_sub_select_conf", "")

    fun playerSpeed() = preferenceStore.getFloat("pref_player_speed", 1F)

    fun aspectState() = preferenceStore.getEnum("pref_player_aspect_state", AspectState.FIT)

    fun screenshotSubtitles() = preferenceStore.getBoolean("pref_screenshot_subtitles", false)

    fun playerStatisticsPage() = preferenceStore.getInt("pref_player_statistics_page", 0)

    fun hardwareDecoding() = preferenceStore.getEnum("pref_hardware_decoding", HwDecState.defaultHwDec)

    fun rememberAudioDelay() = preferenceStore.getBoolean("pref_remember_audio_delay", false)
    fun audioDelay() = preferenceStore.getInt("pref_audio_delay", 0)

    fun rememberSubtitlesDelay() = preferenceStore.getBoolean(
        "pref_remember_subtitles_delay",
        false,
    )
    fun subtitlesDelay() = preferenceStore.getInt("pref_subtitles_delay", 0)

    fun overrideSubsASS() = preferenceStore.getBoolean("pref_override_subtitles_ass", false)

    fun subtitleFont() = preferenceStore.getString("pref_subtitle_font", "Sans Serif")

    fun subtitleFontSize() = preferenceStore.getInt("pref_subtitles_font_size", 55)
    fun boldSubtitles() = preferenceStore.getBoolean("pref_bold_subtitles", false)
    fun italicSubtitles() = preferenceStore.getBoolean("pref_italic_subtitles", false)

    fun textColorSubtitles() = preferenceStore.getInt("pref_text_color_subtitles", -1)
    fun borderColorSubtitles() = preferenceStore.getInt("pref_border_color_subtitles", -16777216)
    fun backgroundColorSubtitles() = preferenceStore.getInt("pref_background_color_subtitles", 0)

    fun brightnessFilter() = preferenceStore.getInt("pref_player_filter_brightness")
    fun saturationFilter() = preferenceStore.getInt("pref_player_filter_saturation")
    fun contrastFilter() = preferenceStore.getInt("pref_player_filter_contrast")
    fun gammaFilter() = preferenceStore.getInt("pref_player_filter_gamma")
    fun hueFilter() = preferenceStore.getInt("pref_player_filter_hue")
}
