package eu.kanade.tachiyomi.ui.player.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.tachiyomi.ui.player.controls.components.panels.SubtitlesBorderStyle
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class SubtitlePreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun preferredSubLanguages() = preferenceStore.getString("pref_subtitle_lang", "")
    fun subtitleWhitelist() = preferenceStore.getString("pref_subtitle_whitelist", "")
    fun subtitleBlacklist() = preferenceStore.getString("pref_subtitle_blacklist", "")

    // Non-preferences

    fun screenshotSubtitles() = preferenceStore.getBoolean("pref_screenshot_subtitles", false)

    fun subtitleFont() = preferenceStore.getString("pref_subtitle_font", "Sans Serif")
    fun subtitleFontSize() = preferenceStore.getInt("pref_subtitles_font_size", 55)
    fun subtitleFontScale() = preferenceStore.getFloat("pref_sub_scale", 1f)
    fun subtitleBorderSize() = preferenceStore.getInt("pref_sub_border_size", 3)
    fun boldSubtitles() = preferenceStore.getBoolean("pref_bold_subtitles", false)
    fun italicSubtitles() = preferenceStore.getBoolean("pref_italic_subtitles", false)

    fun textColorSubtitles() = preferenceStore.getInt("pref_text_color_subtitles", Color.White.toArgb())

    fun borderColorSubtitles() = preferenceStore.getInt("pref_border_color_subtitles", Color.Black.toArgb())
    fun borderStyleSubtitles() = preferenceStore.getEnum(
        "pref_border_style_subtitles",
        SubtitlesBorderStyle.OutlineAndShadow,
    )
    fun shadowOffsetSubtitles() = preferenceStore.getInt("sub_shadow_offset", 0)
    fun backgroundColorSubtitles() = preferenceStore.getInt(
        "pref_background_color_subtitles",
        Color.Transparent.toArgb(),
    )

    fun subtitleJustification() = preferenceStore.getEnum("pref_sub_justify", SubtitleJustification.Auto)
    fun subtitlePos() = preferenceStore.getInt("pref_sub_pos", 100)

    fun overrideSubsASS() = preferenceStore.getBoolean("pref_override_subtitles_ass", false)

    fun subtitlesDelay() = preferenceStore.getInt("pref_subtitles_delay", 0)
    fun subtitlesSpeed() = preferenceStore.getFloat("pref_subtitles_speed", 1f)
    fun subtitlesSecondaryDelay() = preferenceStore.getInt("pref_subtitles_secondary_delay", 0)
}

enum class SubtitleJustification(
    val value: String,
    val icon: ImageVector,
) {
    Left("left", Icons.AutoMirrored.Default.FormatAlignLeft),
    Center("center", Icons.Default.FormatAlignCenter),
    Right("right", Icons.AutoMirrored.Default.FormatAlignRight),
    Auto("auto", Icons.Default.FormatAlignJustify),
}
