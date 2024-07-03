package mihon.core.migration.migrations

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.ui.player.viewer.AspectState
import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import eu.kanade.tachiyomi.ui.player.viewer.InvertedPlayback
import eu.kanade.tachiyomi.ui.player.viewer.VideoDebanding
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class EnumsMigration : Migration {
    override val version = 123f

    // refactor(player): Implement more enums
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val invertedPosition = preferenceStore.getBoolean("pref_invert_playback_txt", false)
        val invertedDuration = preferenceStore.getBoolean("pref_invert_duration_txt", false)
        val hwDec = preferenceStore.getString("pref_hwdec", HwDecState.defaultHwDec.mpvValue)
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

            val invertedPlayback = when {
                invertedPosition.get() -> InvertedPlayback.POSITION
                invertedDuration.get() -> InvertedPlayback.DURATION
                else -> InvertedPlayback.NONE
            }
            val hardwareDecoding = HwDecState.entries.first { it.mpvValue == hwDec.get() }
            val videoDebanding = VideoDebanding.entries.first { it.ordinal == deband.get() }
            val aspectState = AspectState.entries.first { it.ordinal == playerViewMode.get() }

            preferenceStore.getEnum("pref_inverted_playback", InvertedPlayback.NONE).set(invertedPlayback)
            preferenceStore.getEnum("pref_hardware_decoding", HwDecState.defaultHwDec).set(hardwareDecoding)
            preferenceStore.getEnum("pref_video_debanding", VideoDebanding.DISABLED).set(videoDebanding)
            preferenceStore.getEnum("pref_player_aspect_state", AspectState.FIT).set(aspectState)
            preferenceStore.getBoolean("pref_gpu_next", false).set(gpuNext.get())
        }

        return true
    }
}
