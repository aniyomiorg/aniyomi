package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.source.MangaSource
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaSourcesBackupCreator(
    private val mangaSourceManager: MangaSourceManager = Injekt.get(),
) {

    operator fun invoke(mangas: List<BackupManga>): List<BackupSource> {
        return mangas
            .asSequence()
            .map(BackupManga::source)
            .distinct()
            .map(mangaSourceManager::getOrStub)
            .map { it.toBackupSource() }
            .toList()
    }
}

private fun MangaSource.toBackupSource() =
    BackupSource(
        name = this.name,
        sourceId = this.id,
    )
