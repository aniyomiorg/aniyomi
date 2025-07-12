/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.player.settings.DecoderPreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR

/**
 * Results of the set as art feature.
 */
enum class SetAsArt {
    Success,
    AddToLibraryFirst,
    Error,
}

enum class ArtType {
    Cover,
    Background,
    Thumbnail,
}

enum class PlayerOrientation(val titleRes: StringResource) {
    Free(MR.strings.rotation_free),
    Video(AYMR.strings.rotation_video),
    Portrait(MR.strings.rotation_portrait),
    ReversePortrait(MR.strings.rotation_reverse_portrait),
    SensorPortrait(AYMR.strings.rotation_sensor_portrait),
    Landscape(MR.strings.rotation_landscape),
    ReverseLandscape(AYMR.strings.rotation_reverse_landscape),
    SensorLandscape(AYMR.strings.rotation_sensor_landscape),
}

enum class VideoAspect(val titleRes: StringResource) {
    Crop(AYMR.strings.video_crop_screen),
    Fit(AYMR.strings.video_fit_screen),
    Stretch(AYMR.strings.video_stretch_screen),
}

/**
 * Action performed by a button, like double tap or media controls
 */
enum class SingleActionGesture(val stringRes: StringResource) {
    None(stringRes = AYMR.strings.single_action_none),
    Seek(stringRes = AYMR.strings.single_action_seek),
    PlayPause(stringRes = AYMR.strings.single_action_playpause),
    Switch(stringRes = AYMR.strings.single_action_switch),
    Custom(stringRes = AYMR.strings.single_action_custom),
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
    More,
    Screenshot,
}

enum class Panels {
    None,
    SubtitleSettings,
    SubtitleDelay,
    AudioDelay,
    VideoFilters,
}

sealed class Dialogs {
    data object None : Dialogs()
    data object EpisodeList : Dialogs()
    data class IntegerPicker(
        val defaultValue: Int,
        val minValue: Int,
        val maxValue: Int,
        val step: Int,
        val nameFormat: String,
        val title: String,
        val onChange: (Int) -> Unit,
        val onDismissRequest: () -> Unit,
    ) : Dialogs()
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
        AYMR.strings.player_sheets_filters_brightness,
        { it.brightnessFilter() },
        "brightness",
    ),
    SATURATION(
        AYMR.strings.player_sheets_filters_Saturation,
        { it.saturationFilter() },
        "saturation",
    ),
    CONTRAST(
        AYMR.strings.player_sheets_filters_contrast,
        { it.contrastFilter() },
        "contrast",
    ),
    GAMMA(
        AYMR.strings.player_sheets_filters_gamma,
        { it.gammaFilter() },
        "gamma",
    ),
    HUE(
        AYMR.strings.player_sheets_filters_hue,
        { it.hueFilter() },
        "hue",
    ),
}
