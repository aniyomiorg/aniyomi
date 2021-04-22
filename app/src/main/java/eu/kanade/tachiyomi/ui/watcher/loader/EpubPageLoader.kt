package eu.kanade.tachiyomi.ui.watcher.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.util.storage.EpubFile
import rx.Observable
import java.io.File

/**
 * Loader used to load a episode from a .epub file.
 */
class EpubPageLoader(file: File) : PageLoader() {

    /**
     * The epub file.
     */
    private val epub = EpubFile(file)

    /**
     * Recycles this loader and the open zip.
     */
    override fun recycle() {
        super.recycle()
        epub.close()
    }

    /**
     * Returns an observable containing the pages found on this zip archive ordered with a natural
     * comparator.
     */
    override fun getPages(): Observable<List<WatcherPage>> {
        return epub.getImagesFromPages()
            .mapIndexed { i, path ->
                val streamFn = { epub.getInputStream(epub.getEntry(path)!!) }
                WatcherPage(i).apply {
                    stream = streamFn
                    status = Page.READY
                }
            }
            .let { Observable.just(it) }
    }

    /**
     * Returns an observable that emits a ready state unless the loader was recycled.
     */
    override fun getPage(page: WatcherPage): Observable<Int> {
        return Observable.just(
            if (isRecycled) {
                Page.ERROR
            } else {
                Page.READY
            }
        )
    }
}
