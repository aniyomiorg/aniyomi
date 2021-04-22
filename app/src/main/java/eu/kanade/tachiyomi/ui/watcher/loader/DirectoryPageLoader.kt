package eu.kanade.tachiyomi.ui.watcher.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.system.ImageUtil
import rx.Observable
import java.io.File
import java.io.FileInputStream

/**
 * Loader used to load a episode from a directory given on [file].
 */
class DirectoryPageLoader(val file: File) : PageLoader() {

    /**
     * Returns an observable containing the pages found on this directory ordered with a natural
     * comparator.
     */
    override fun getPages(): Observable<List<WatcherPage>> {
        return file.listFiles()
            .filter { !it.isDirectory && ImageUtil.isImage(it.name) { FileInputStream(it) } }
            .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
            .mapIndexed { i, file ->
                val streamFn = { FileInputStream(file) }
                WatcherPage(i).apply {
                    stream = streamFn
                    status = Page.READY
                }
            }
            .let { Observable.just(it) }
    }

    /**
     * Returns an observable that emits a ready state.
     */
    override fun getPage(page: WatcherPage): Observable<Int> {
        return Observable.just(Page.READY)
    }
}
