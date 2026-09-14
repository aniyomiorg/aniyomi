package eu.kanade.domain.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.domain.ui.model.StartScreen
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.presentation.core.util.collectAsState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun themeMode() = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    fun appTheme() = preferenceStore.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) {
            AppTheme.MONET
        } else {
            AppTheme.DEFAULT
        },
    )

    fun themeDarkAmoled() = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    fun relativeTime() = preferenceStore.getBoolean("relative_time_v2", true)

    fun dateFormat() = preferenceStore.getString("app_date_format", "")

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun startScreen() = preferenceStore.getEnum("start_screen", StartScreen.ANIME)

    fun hideManga() = preferenceStore.getBoolean("hide_manga", false)

    // This class wraps the bottom_rail_nav_style preference, always acting as if it is set to
    // NavStyle.MOVE_MANGA_TO_MORE when the hide_manga preference is set. This allows most consumers to
    // interact with the result of navStyle() the same way they interact with the reset of the preferences
    // in this file.
    inner class EffectiveNavStyle internal constructor() {
        fun underlyingPreference() = preferenceStore.getEnum(
            "bottom_rail_nav_style",
            NavStyle.MOVE_HISTORY_TO_MORE,
        )

        @Composable
        fun collectAsState(): State<NavStyle> {
            val underlyingNavStyleState = underlyingPreference()
            val underlyingNavStyleFlow = remember(underlyingNavStyleState) { underlyingNavStyleState.changes() }
            val hideMangaState = hideManga()
            val hideMangaFlow = remember(hideMangaState) { hideMangaState.changes() }

            return combine<NavStyle, Boolean, NavStyle>(
                underlyingNavStyleFlow,
                hideMangaFlow,
            ) { underlyingNavStyleValue, hideMangaValue ->
                if (hideMangaValue) {
                    NavStyle.MOVE_MANGA_TO_MORE
                } else {
                    underlyingNavStyleValue
                }
            }.collectAsState(get())
        }

        fun get(): NavStyle = if (hideManga().get()) {
            NavStyle.MOVE_MANGA_TO_MORE
        } else {
            underlyingPreference().get()
        }
    }

    fun navStyle() = EffectiveNavStyle()

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}
