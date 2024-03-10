package eu.kanade.tachiyomi.data.download.anime.model

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.ProgressListener
import okhttp3.Request

/**
 * This class contains all data needed to manage the download of a http range download part
 */
class AnimeDownloadPart(
    placingDir: UniFile,
    range: Pair<Long, Long>,
) {
    /**
     * Directory where to place the download file
     */
    private val placingDir: UniFile

    /**
     * The download file
     */
    private var _file: UniFile? = null

    /**
     * The range of bytes this part covers
     */
    var range: Pair<Long, Long>

    init {
        this.range = range
        this.placingDir = placingDir
    }

    /**
     * If the download of this part has been completed or not
     */
    @Volatile
    var completed: Boolean = false

    /**
     * Retrieve a valid download file (creates one if there isn't yet one)
     */
    var file: UniFile
        get() {
            if (_file == null) {
                _file = placingDir.createFile("${range.first}.part.tmp")!!
            }
            return _file!!
        }
        set(value) {
            _file = value
        }

    /**
     * If present it the request of the not yet downloaded bytes range
     */
    var request: Request? = null

    var listener: ProgressListener? = null
}
