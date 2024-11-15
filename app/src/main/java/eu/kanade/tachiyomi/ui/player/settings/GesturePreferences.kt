package eu.kanade.tachiyomi.ui.player.settings

import eu.kanade.tachiyomi.ui.player.SingleActionGesture
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class GesturePreferences(
    private val preferenceStore: PreferenceStore,
) {
    // Seeking

    fun gestureHorizontalSeek() = preferenceStore.getBoolean("pref_gesture_horizontal_seek", true)
    fun showSeekBar() = preferenceStore.getBoolean("pref_show_seekbar", false)
    fun defaultIntroLength() = preferenceStore.getInt("pref_default_intro_length", 85)
    fun skipLengthPreference() = preferenceStore.getInt("pref_skip_length_preference", 10)
    fun playerSmoothSeek() = preferenceStore.getBoolean("pref_player_smooth_seek", false)
    fun mediaChapterSeek() = preferenceStore.getBoolean("pref_media_control_chapter_seeking", false)

    fun aniSkipEnabled() = preferenceStore.getBoolean("pref_enable_ani_skip", false)
    fun autoSkipAniSkip() = preferenceStore.getBoolean("pref_enable_auto_skip_ani_skip", false)
    fun enableNetflixStyleAniSkip() = preferenceStore.getBoolean(
        "pref_enable_netflixStyle_aniskip",
        false,
    )
    fun waitingTimeAniSkip() = preferenceStore.getInt("pref_waiting_time_aniskip", 5)

    // Double tap

    fun leftDoubleTapGesture() = preferenceStore.getEnum("pref_left_double_tap", SingleActionGesture.Seek)
    fun centerDoubleTapGesture() = preferenceStore.getEnum("pref_center_double_tap", SingleActionGesture.PlayPause)
    fun rightDoubleTapGesture() = preferenceStore.getEnum("pref_right_double_tap", SingleActionGesture.Seek)

    // Media controls

    fun mediaPreviousGesture() = preferenceStore.getEnum("pref_media_previous", SingleActionGesture.Switch)
    fun mediaPlayPauseGesture() = preferenceStore.getEnum("pref_media_playpause", SingleActionGesture.PlayPause)
    fun mediaNextGesture() = preferenceStore.getEnum("pref_media_next", SingleActionGesture.Switch)
}
