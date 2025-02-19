package mihon.core.migration.migrations

import android.app.Application
import android.os.Build
import android.os.Environment
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.domain.storage.service.StorageManager
import java.io.File

class PEMFileMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val storageManager = migrationContext.get<StorageManager>() ?: return false
        val context = migrationContext.get<Application>() ?: return false

        val configDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            storageManager.getMPVConfigDirectory()!!.filePath!!
        } else {
            context.applicationContext.filesDir.path
        }

        val pemFile = File(configDir, "cacert.pem")
        if (pemFile.exists()) {
            pemFile.delete()
        }

        return true
    }
}
