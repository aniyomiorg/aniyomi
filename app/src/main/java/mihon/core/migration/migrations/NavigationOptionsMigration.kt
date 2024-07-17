package mihon.core.migration.migrations

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.domain.ui.model.NavStyle
import eu.kanade.domain.ui.model.StartScreen
import eu.kanade.tachiyomi.App
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class NavigationOptionsMigration : Migration {
    override val version = 120f

    // Bring back navigation options
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val bottomNavStyle = preferenceStore.getInt("bottom_nav_style", 0)

        val isDefaultTabManga = preferenceStore.getBoolean("default_home_tab_library", false)
        prefs.edit {
            remove("bottom_nav_style")
            remove("default_home_tab_library")

            val startScreen = if (isDefaultTabManga.get()) StartScreen.MANGA else StartScreen.ANIME
            val navStyle = when (bottomNavStyle.get()) {
                0 -> NavStyle.MOVE_HISTORY_TO_MORE
                1 -> NavStyle.MOVE_UPDATES_TO_MORE
                else -> NavStyle.MOVE_MANGA_TO_MORE
            }

            preferenceStore.getEnum("start_screen", StartScreen.ANIME).set(startScreen)
            preferenceStore.getEnum("bottom_rail_nav_style", NavStyle.MOVE_HISTORY_TO_MORE).set(navStyle)
        }

        return true
    }
}
