package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupExtensionRepos
import eu.kanade.tachiyomi.data.backup.models.backupExtensionReposMapper
import mihon.domain.extensionrepo.manga.interactor.GetMangaExtensionRepo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaExtensionRepoBackupCreator(
    private val getMangaExtensionRepos: GetMangaExtensionRepo = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupExtensionRepos> {
        return getMangaExtensionRepos.getAll()
            .map(backupExtensionReposMapper)
    }
}
