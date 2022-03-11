package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.track.TrackManager
import uy.kohesive.injekt.injectLazy

abstract class AbstractBackupRestoreValidator {
    protected val animesourceManager: AnimeSourceManager by injectLazy()
    protected val trackManager: TrackManager by injectLazy()

    abstract fun validate(context: Context, uri: Uri): Results

    data class Results(val missingSources: List<String>, val missingTrackers: List<String>)
}
