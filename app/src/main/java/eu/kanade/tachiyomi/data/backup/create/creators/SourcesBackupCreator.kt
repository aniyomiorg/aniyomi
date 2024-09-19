package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.source.MangaSource
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcesBackupCreator(
    private val animeSourceManager: AnimeSourceManager = Injekt.get(),
    private val mangaSourceManager: MangaSourceManager = Injekt.get(),
) {

    fun backupAnimeSources(animes: List<BackupAnime>): List<BackupAnimeSource> {
        return animes
            .asSequence()
            .map(BackupAnime::source)
            .distinct()
            .map(animeSourceManager::getOrStub)
            .map { it.toBackupSource() }
            .toList()
    }

    fun backupMangaSources(mangas: List<BackupManga>): List<BackupSource> {
        return mangas
            .asSequence()
            .map(BackupManga::source)
            .distinct()
            .map(mangaSourceManager::getOrStub)
            .map { it.toBackupSource() }
            .toList()
    }
}

private fun AnimeSource.toBackupSource() =
    BackupAnimeSource(
        name = this.name,
        sourceId = this.id,
    )

private fun MangaSource.toBackupSource() =
    BackupSource(
        name = this.name,
        sourceId = this.id,
    )
