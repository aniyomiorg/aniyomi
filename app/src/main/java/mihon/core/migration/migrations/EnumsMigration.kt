package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.player.Debanding
import eu.kanade.tachiyomi.ui.player.VideoAspect
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class EnumsMigration : Migration {
    override val version = 123f

    // refactor(player): Implement more enums
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val deband = preferenceStore.getInt("pref_deband", 0)
        val playerViewMode = preferenceStore.getInt("pref_player_view_mode", 1)
        val gpuNext = preferenceStore.getBoolean("gpu_next", false)

        prefs.edit {
            remove("pref_invert_playback_txt")
            remove("pref_invert_duration_txt")
            remove("pref_hwdec")
            remove("pref_deband")
            remove("pref_player_view_mode")
            remove("gpu_next")

            val videoDebanding = Debanding.entries.first { it.ordinal == deband.get() }
            val aspectState = VideoAspect.entries.first { it.ordinal == playerViewMode.get() }

            preferenceStore.getEnum("pref_video_debanding", Debanding.None).set(videoDebanding)
            preferenceStore.getEnum("pref_player_aspect_state", VideoAspect.Fit).set(aspectState)
            preferenceStore.getBoolean("pref_gpu_next", false).set(gpuNext.get())
        }

        return true
    }
}
