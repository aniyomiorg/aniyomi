package mihon.core.migration.migrations

import androidx.preference.PreferenceManager
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.util.system.DeviceUtil
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class MigrateSecureScreenMigration : Migration {
    override val version = 75f

    // Allow disabling secure screen when incognito mode is on
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val securityPreferences = migrationContext.get<SecurityPreferences>() ?: return false
        val basePreferences = migrationContext.get<BasePreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val oldSecureScreen = prefs.getBoolean("secure_screen", false)
        if (oldSecureScreen) {
            securityPreferences.secureScreen().set(
                SecurityPreferences.SecureScreenMode.ALWAYS,
            )
        }
        if (DeviceUtil.isMiui &&
            basePreferences.extensionInstaller().get() == BasePreferences.ExtensionInstaller.PACKAGEINSTALLER
        ) {
            basePreferences.extensionInstaller().set(
                BasePreferences.ExtensionInstaller.LEGACY,
            )
        }

        return true
    }
}
