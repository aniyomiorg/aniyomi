package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupExtensionRepos
import eu.kanade.tachiyomi.data.backup.models.backupExtensionReposMapper
import mihon.domain.extensionrepo.anime.interactor.GetAnimeExtensionRepo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionRepoBackupCreator(
    private val getAnimeExtensionRepos: GetAnimeExtensionRepo = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupExtensionRepos> {
        return getAnimeExtensionRepos.getAll()
            .map(backupExtensionReposMapper)
    }
}
