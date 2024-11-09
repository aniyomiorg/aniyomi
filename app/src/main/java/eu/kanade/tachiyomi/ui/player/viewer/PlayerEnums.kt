package eu.kanade.tachiyomi.ui.player.viewer

import android.os.Build
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/**
 * Results of the set as cover feature.
 */
enum class SetAsCover {
    Success,
    AddToLibraryFirst,
    Error,
}

/**
 * Player's inverted playback text handler
 */
enum class InvertedPlayback {
    NONE,
    POSITION,
    DURATION,
}

/**
 * Player's Picture-In-Picture state handler
 */
enum class PipState {
    OFF,
    ON,
    STARTED,
    ;

    companion object {
        internal var mode: PipState = OFF
    }
}

/**
 * Player's Seek state handler
 */
enum class SeekState {
    DOUBLE_TAP,
    LOCKED,
    NONE,
    SCROLL,
    SEEKBAR,
    ;

    companion object {
        internal var mode = NONE
    }
}

/**
 * Player's Video Aspect state handler
 */
enum class AspectState(val stringRes: StringResource) {
    CROP(stringRes = MR.strings.video_crop_screen),
    FIT(stringRes = MR.strings.video_fit_screen),
    STRETCH(stringRes = MR.strings.video_stretch_screen),
    CUSTOM(stringRes = MR.strings.video_custom_screen),
    ;

    companion object {
        internal var mode: AspectState = FIT
    }
}

/**
 * Player's Hardware Decoder type handler
 */
enum class HwDecState(val title: String, val mpvValue: String) {
    HW_PLUS(title = "HW+", mpvValue = "mediacodec"),
    HW(title = "HW", mpvValue = "mediacodec-copy"),
    SW(title = "SW", mpvValue = "no"),
    ;

    companion object {
        private val isWSA = Build.MODEL == "Subsystem for Android(TM)" ||
            Build.BRAND == "Windows" ||
            Build.BOARD == "windows"

        internal val defaultHwDec = when {
            isWSA -> SW
            else -> HW_PLUS
        }
    }
}

/**
 * Player's Statistics Page handler
 */
@Suppress("unused")
enum class PlayerStatsPage(val stringRes: StringResource) {
    OFF(stringRes = MR.strings.off),
    PAGE1(stringRes = MR.strings.player_statistics_page_1),
    PAGE2(stringRes = MR.strings.player_statistics_page_2),
    PAGE3(stringRes = MR.strings.player_statistics_page_3),
}

/**
 * Player's debanding handler
 */
enum class VideoDebanding(val stringRes: StringResource) {
    NONE(stringRes = MR.strings.pref_debanding_none),
    CPU(stringRes = MR.strings.pref_debanding_cpu),
    GPU(stringRes = MR.strings.pref_debanding_gpu),
}

/**
 * Action performed by a button, like double tap or media controls
 */
enum class SingleActionGesture(val stringRes: StringResource) {
    None(stringRes = MR.strings.single_action_none),
    Seek(stringRes = MR.strings.single_action_seek),
    PlayPause(stringRes = MR.strings.single_action_playpause),
    Switch(stringRes = MR.strings.single_action_switch),
    Custom(stringRes = MR.strings.single_action_custom),
}

/**
 * Key codes sent through the `Custom` option in gestures
 */
enum class CustomKeyCodes(val keyCode: String) {
    DoubleTapLeft("0x10001"),
    DoubleTapCenter("0x10002"),
    DoubleTapRight("0x10003"),
    MediaPrevious("0x10004"),
    MediaPlay("0x10005"),
    MediaNext("0x10006"),
}

enum class AudioChannels(val propertyName: String, val propertyValue: String, val textRes: StringResource) {
    AutoSafe("audio-channels", "auto-safe", MR.strings.pref_player_audio_channels_auto_safe),
    Auto("audio-channels", "auto", MR.strings.pref_player_audio_channels_auto),
    Mono("audio-channels", "mono", MR.strings.pref_player_audio_channels_mono),
    Stereo("audio-channels", "stereo", MR.strings.pref_player_audio_channels_stereo),
    ReverseStereo(
        "af",
        "pan=[stereo|c0=c1|c1=c0]",
        MR.strings.pref_player_audio_channels_reverse_stereo,
    ),
}
