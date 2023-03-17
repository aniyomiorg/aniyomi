package eu.kanade.data.track.manga

import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.track.manga.model.MangaTrack
import eu.kanade.domain.track.manga.repository.MangaTrackRepository
import kotlinx.coroutines.flow.Flow

class MangaTrackRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : MangaTrackRepository {

    override suspend fun getTrackByMangaId(id: Long): MangaTrack? {
        return handler.awaitOneOrNull { manga_syncQueries.getTrackById(id, mangaTrackMapper) }
    }

    override suspend fun getTracksByMangaId(mangaId: Long): List<MangaTrack> {
        return handler.awaitList {
            manga_syncQueries.getTracksByMangaId(mangaId, mangaTrackMapper)
        }
    }

    override fun getMangaTracksAsFlow(): Flow<List<MangaTrack>> {
        return handler.subscribeToList {
            manga_syncQueries.getTracks(mangaTrackMapper)
        }
    }

    override fun getTracksByMangaIdAsFlow(mangaId: Long): Flow<List<MangaTrack>> {
        return handler.subscribeToList {
            manga_syncQueries.getTracksByMangaId(mangaId, mangaTrackMapper)
        }
    }

    override suspend fun deleteManga(mangaId: Long, syncId: Long) {
        handler.await {
            manga_syncQueries.delete(
                mangaId = mangaId,
                syncId = syncId,
            )
        }
    }

    override suspend fun insertManga(track: MangaTrack) {
        insertValues(track)
    }

    override suspend fun insertAllManga(tracks: List<MangaTrack>) {
        insertValues(*tracks.toTypedArray())
    }

    private suspend fun insertValues(vararg tracks: MangaTrack) {
        handler.await(inTransaction = true) {
            tracks.forEach { mangaTrack ->
                manga_syncQueries.insert(
                    mangaId = mangaTrack.mangaId,
                    syncId = mangaTrack.syncId,
                    remoteId = mangaTrack.remoteId,
                    libraryId = mangaTrack.libraryId,
                    title = mangaTrack.title,
                    lastChapterRead = mangaTrack.lastChapterRead,
                    totalChapters = mangaTrack.totalChapters,
                    status = mangaTrack.status,
                    score = mangaTrack.score,
                    remoteUrl = mangaTrack.remoteUrl,
                    startDate = mangaTrack.startDate,
                    finishDate = mangaTrack.finishDate,
                )
            }
        }
    }
}
