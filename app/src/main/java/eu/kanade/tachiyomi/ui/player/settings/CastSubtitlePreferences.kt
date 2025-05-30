package eu.kanade.tachiyomi.ui.player.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import eu.kanade.tachiyomi.ui.player.cast.components.BorderStyle
import eu.kanade.tachiyomi.ui.player.cast.components.SubtitleSettings
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat

class CastSubtitlePreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun fontSize() = preferenceStore.getFloat("pref_cast_subtitle_size", 20f)
    fun textColor() = preferenceStore.getInt("pref_cast_subtitle_color", Color.White.toArgb())
    fun backgroundColor() = preferenceStore.getInt("pref_cast_subtitle_background", Color.Transparent.toArgb())
    fun shadowRadius() = preferenceStore.getFloat("pref_cast_subtitle_shadow", 2f)
    private fun fontFamily() = preferenceStore.getString("pref_cast_subtitle_font_family", "SANS_SERIF")
    fun borderStyle() = preferenceStore.getString("pref_cast_subtitle_border_style", "NONE")

    private fun fontFamilyToString(fontFamily: FontFamily): String {
        return when (fontFamily) {
            FontFamily.Default -> "SANS_SERIF"
            FontFamily.SansSerif -> "SANS_SERIF"
            FontFamily.Serif -> "SERIF"
            FontFamily.Monospace -> "MONOSPACE"
            FontFamily.Cursive -> "CURSIVE"
            else -> "SANS_SERIF"
        }
    }

    private fun stringToFontFamily(fontFamilyStr: String): FontFamily {
        return when (fontFamilyStr) {
            "SERIF" -> FontFamily.Serif
            "MONOSPACE" -> FontFamily.Monospace
            "CURSIVE" -> FontFamily.Cursive
            else -> FontFamily.SansSerif
        }
    }

    fun saveTextTrackStyle(settings: SubtitleSettings) {
        try {
            preferenceStore.getFloat("pref_cast_subtitle_size", 20f).set(settings.fontSize.value)
            preferenceStore.getInt("pref_cast_subtitle_color", Color.White.toArgb()).set(settings.textColor.toArgb())
            preferenceStore.getInt("pref_cast_subtitle_background", Color.Transparent.toArgb())
                .set(settings.backgroundColor.toArgb())
            preferenceStore.getFloat("pref_cast_subtitle_shadow", 2f).set(settings.shadowRadius.value)
            preferenceStore.getString("pref_cast_subtitle_font_family", "SANS_SERIF")
                .set(fontFamilyToString(settings.fontFamily))
            preferenceStore.getString("pref_cast_subtitle_border_style", "NONE")
                .set(settings.borderStyle.name)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Error saving subtitle settings: ${e.message}" }
        }
    }

    // Método para obtener la fuente guardada
    fun getFontFamily(): FontFamily {
        return stringToFontFamily(fontFamily().get())
    }

    fun getBorderStyle(): BorderStyle {
        return try {
            BorderStyle.valueOf(borderStyle().get())
        } catch (e: Exception) {
            BorderStyle.NONE
        }
    }
}
