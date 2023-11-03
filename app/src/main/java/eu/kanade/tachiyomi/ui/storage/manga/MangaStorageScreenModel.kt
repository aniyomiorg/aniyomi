package eu.kanade.tachiyomi.ui.storage.manga

import cafe.adriel.voyager.core.model.coroutineScope
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadCache
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.ui.storage.CommonStorageScreenModel
import eu.kanade.tachiyomi.util.size
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.domain.category.manga.interactor.GetVisibleMangaCategories
import tachiyomi.domain.entries.manga.interactor.GetLibraryManga
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.source.local.entries.manga.isLocal
import tachiyomi.source.local.io.manga.LocalMangaSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaStorageScreenModel(
    downloadCache: MangaDownloadCache = Injekt.get(),
    private val getLibraries: GetLibraryManga = Injekt.get(),
    getVisibleCategories: GetVisibleMangaCategories = Injekt.get(),
    private val downloadManager: MangaDownloadManager = Injekt.get(),
    private val sourceManager: MangaSourceManager = Injekt.get(),
    private val localManager: LocalMangaSourceFileSystem = Injekt.get(),
) : CommonStorageScreenModel<LibraryManga>(
    downloadCacheChanges = downloadCache.changes,
    downloadCacheIsInitializing = downloadCache.isInitializing,
    libraries = getLibraries.subscribe(),
    categories = getVisibleCategories.subscribe(),
    getTotalDownloadSize = { downloadManager.getDownloadSize() },
    getDownloadSize = {
        if (sourceManager.getOrStub(manga.source).isLocal()) {
            localManager.getChaptersInMangaDirectory(manga.title)
                .map { UniFile.fromFile(it)?.size() ?: 0 }
                .sum()
        } else {
            downloadManager.getDownloadSize(manga)
        }
    },
    getDownloadCount = {
        if (sourceManager.getOrStub(manga.source).isLocal()) {
            localManager.getChaptersInMangaDirectory(manga.title).count()
        } else {
            downloadManager.getDownloadCount(manga)
        }
    },
    getId = { id },
    getCategoryId = { category },
    getTitle = { manga.title },
    getThumbnail = { manga.thumbnailUrl },
    isFromLocalSource = { sourceManager.getOrStub(manga.source).isLocal() },
) {
    override fun deleteEntry(id: Long) {
        coroutineScope.launchNonCancellable {
            val manga = getLibraries.await().find {
                it.id == id
            }?.manga ?: return@launchNonCancellable
            val source = sourceManager.get(manga.source) ?: return@launchNonCancellable
            downloadManager.deleteManga(manga, source)
        }
    }
}
