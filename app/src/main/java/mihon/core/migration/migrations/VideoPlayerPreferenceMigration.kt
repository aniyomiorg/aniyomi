package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import uy.kohesive.injekt.injectLazy

class VideoPlayerPreferenceMigration : Migration {
    override val version = 126f

    private val json: Json by injectLazy()

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val subtitlePreferences = migrationContext.get<SubtitlePreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val subtitleConf = prefs.getString("pref_sub_select_conf", "")!!
        val subtitleData = try {
            json.decodeFromString<SubConfig>(subtitleConf)
        } catch (e: SerializationException) {
            return false
        }

        prefs.edit {
            putString(subtitlePreferences.preferredSubLanguages().key(), subtitleData.lang.joinToString(","))
            putString(subtitlePreferences.subtitleWhitelist().key(), subtitleData.whitelist.joinToString(","))
            putString(subtitlePreferences.subtitleBlacklist().key(), subtitleData.blacklist.joinToString(","))
        }

        return true
    }

    @Serializable
    data class SubConfig(
        val lang: List<String> = emptyList(),
        val blacklist: List<String> = emptyList(),
        val whitelist: List<String> = emptyList(),
    )
}
