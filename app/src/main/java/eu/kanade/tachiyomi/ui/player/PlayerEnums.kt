package eu.kanade.tachiyomi.ui.player

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.player.settings.DecoderPreferences
import tachiyomi.core.common.preference.Preference
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

enum class PlayerOrientation(val titleRes: StringResource) {
    Free(MR.strings.rotation_free),
    Video(MR.strings.rotation_video),
    Portrait(MR.strings.rotation_portrait),
    ReversePortrait(MR.strings.rotation_reverse_portrait),
    SensorPortrait(MR.strings.rotation_sensor_portrait),
    Landscape(MR.strings.rotation_landscape),
    ReverseLandscape(MR.strings.rotation_reverse_landscape),
    SensorLandscape(MR.strings.rotation_sensor_landscape),
}

enum class VideoAspect(val titleRes: StringResource) {
    Crop(MR.strings.video_crop_screen),
    Fit(MR.strings.video_fit_screen),
    Stretch(MR.strings.video_stretch_screen),
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

enum class Decoder(val title: String, val value: String) {
    AutoCopy("Auto", "auto-copy"),
    Auto("Auto", "auto"),
    SW("SW", "no"),
    HW("HW", "mediacodec-copy"),
    HWPlus("HW+", "mediacodec"),
}

fun getDecoderFromValue(value: String): Decoder {
    return Decoder.entries.first { it.value == value }
}

enum class Debanding {
    None,
    CPU,
    GPU,
}

enum class Sheets {
    None,
    PlaybackSpeed,
    SubtitleTracks,
    AudioTracks,
    QualityTracks,
    Chapters,
    Decoders,
    More,
}

enum class Panels {
    None,
    SubtitleSettings,
    SubtitleDelay,
    AudioDelay,
    VideoFilters,
}

sealed class PlayerUpdates {
    data object None : PlayerUpdates()
    data object DoubleSpeed : PlayerUpdates()
    data object AspectRatio : PlayerUpdates()
    data class ShowText(val value: String) : PlayerUpdates()
    data class ShowTextResource(val textResource: StringResource) : PlayerUpdates()
}

enum class VideoFilters(
    val titleRes: StringResource,
    val preference: (DecoderPreferences) -> Preference<Int>,
    val mpvProperty: String,
) {
    BRIGHTNESS(
        MR.strings.player_sheets_filters_brightness,
        { it.brightnessFilter() },
        "brightness",
    ),
    SATURATION(
        MR.strings.player_sheets_filters_Saturation,
        { it.saturationFilter() },
        "saturation",
    ),
    CONTRAST(
        MR.strings.player_sheets_filters_contrast,
        { it.contrastFilter() },
        "contrast",
    ),
    GAMMA(
        MR.strings.player_sheets_filters_gamma,
        { it.gammaFilter() },
        "gamma",
    ),
    HUE(
        MR.strings.player_sheets_filters_hue,
        { it.hueFilter() },
        "hue",
    ),
}
