package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupExtensionRepos
import mihon.domain.extensionrepo.anime.interactor.GetAnimeExtensionRepo
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionRepoRestorer(
    private val animeHandler: AnimeDatabaseHandler = Injekt.get(),
    private val getExtensionRepos: GetAnimeExtensionRepo = Injekt.get(),
) {

    suspend operator fun invoke(
        backupRepo: BackupExtensionRepos,
    ) {
        val dbRepos = getExtensionRepos.getAll()
        val existingReposBySHA = dbRepos.associateBy { it.signingKeyFingerprint }
        val existingReposByUrl = dbRepos.associateBy { it.baseUrl }
        val urlExists = existingReposByUrl[backupRepo.baseUrl]
        val shaExists = existingReposBySHA[backupRepo.signingKeyFingerprint]
        if (urlExists != null && urlExists.signingKeyFingerprint != backupRepo.signingKeyFingerprint) {
            error("Already Exists with different signing key fingerprint")
        } else if (shaExists != null) {
            error("${shaExists.name} has the same signing key fingerprint")
        } else {
            animeHandler.await {
                extension_reposQueries.insert(
                    backupRepo.baseUrl,
                    backupRepo.name,
                    backupRepo.shortName,
                    backupRepo.website,
                    backupRepo.signingKeyFingerprint
                )
            }
        }
    }
}
