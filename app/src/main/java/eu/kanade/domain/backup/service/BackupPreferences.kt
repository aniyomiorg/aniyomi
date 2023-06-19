package eu.kanade.domain.backup.service

import eu.kanade.tachiyomi.data.preference.FLAG_CATEGORIES
import eu.kanade.tachiyomi.data.preference.FLAG_CHAPTERS
import eu.kanade.tachiyomi.data.preference.FLAG_HISTORY
import eu.kanade.tachiyomi.data.preference.FLAG_TRACK
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.provider.FolderProvider

class BackupPreferences(
    private val folderProvider: FolderProvider,
    private val preferenceStore: PreferenceStore,
) {

    fun backupsDirectory() = preferenceStore.getString("backup_directory", folderProvider.path())

    fun numberOfBackups() = preferenceStore.getInt("backup_slots", 2)

    fun backupInterval() = preferenceStore.getInt("backup_interval", 12)

    fun backupFlags() = preferenceStore.getStringSet("backup_flags", setOf(FLAG_CATEGORIES, FLAG_CHAPTERS, FLAG_HISTORY, FLAG_TRACK))
}
