package eu.kanade.domain.track.manga.interactor

import eu.kanade.domain.track.manga.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.EnhancedMangaTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.source.MangaSource
import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack

class AddMangaTracks(
    private val getTracks: GetMangaTracks,
    private val insertTrack: InsertMangaTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
) {

    suspend fun bindEnhancedTracks(manga: Manga, source: MangaSource) {
        withNonCancellableContext {
            getTracks.await(manga.id)
                .filterIsInstance<EnhancedMangaTracker>()
                .filter { it.accept(source) }
                .forEach { service ->
                    try {
                        service.match(manga)?.let { track ->
                            track.manga_id = manga.id
                            (service as Tracker).mangaService.bind(track)
                            insertTrack.await(track.toDomainTrack()!!)

                            syncChapterProgressWithTrack.await(
                                manga.id,
                                track.toDomainTrack()!!,
                                service.mangaService,
                            )
                        }
                    } catch (e: Exception) {
                        logcat(
                            LogPriority.WARN,
                            e,
                        ) { "Could not match manga: ${manga.title} with service $service" }
                    }
                }
        }
    }
}
