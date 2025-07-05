package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.player.settings.AudioPreferences
import eu.kanade.tachiyomi.ui.player.settings.SubtitlePreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import java.util.Locale
import java.util.MissingResourceException
import kotlin.text.split

class PrefLangMigration : Migration {
    override val version = 130f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val audioPreferences = migrationContext.get<AudioPreferences>() ?: return false
        val subtitlePreferences = migrationContext.get<SubtitlePreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        listOf(
            audioPreferences.preferredAudioLanguages(),
            subtitlePreferences.preferredSubLanguages(),
        ).forEach { pref ->
            if (pref.isSet()) {
                prefs.edit {
                    val langs = prefs.getString(
                        pref.key(),
                        "",
                    )!!.split(",").filter(String::isNotEmpty).map(String::trim)
                    val newLangs = langs.filter { it.isValidCode() }.joinToString(",")
                    putString(pref.key(), newLangs)
                }
            }
        }

        return true
    }

    private fun String.isValidCode(): Boolean {
        try {
            val locale = Locale(this)
            if (locale.isO3Language == locale.language && locale.language == locale.getDisplayName(Locale.ENGLISH)) {
                return false
            }
        } catch (_: MissingResourceException) {
            return false
        }

        return true
    }
}
