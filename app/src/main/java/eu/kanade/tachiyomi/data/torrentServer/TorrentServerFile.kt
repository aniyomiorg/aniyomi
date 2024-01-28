package eu.kanade.tachiyomi.data.torrentServer

import android.app.Application
import android.os.Build
import android.os.Environment
import android.util.Log
import com.topjohnwu.superuser.Shell
import eu.kanade.tachiyomi.BuildConfig
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class TorrentServerFile : File(Injekt.get<Application>().filesDir, "torrserver") {
    private val lock = Any()
    private val applicationContext = Injekt.get<Application>()
    private val preferences = Injekt.get<TorrentServerPreferences>()
    private val torrPath = getTorrPath()
    private val logfile = File(torrPath, "torrserver.log").path
    private var shellJob: Shell.Job? = null
    private var shell: Shell? = null

    fun run() {
        if (!exists()) {
            return
        }
        synchronized(lock) {
            val port = preferences.port().get()
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            shell =
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_NON_ROOT_SHELL)
                    .build()
            shellJob =
                shell!!.newJob()
                    .add("export GODEBUG=madvdontneed=1")
                    .add("$path -k --path $torrPath --port $port --logpath $logfile 1>>$logfile 2>&1 &")
            shellJob!!.exec()
        }
    }

    fun stop() {
        if (!exists()) {
            return
        }
        synchronized(lock) {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            shellJob?.add("killall torrserver")?.exec()
            shell?.close()
            TorrentServerApi.shutdown()
        }
    }

    private fun getTorrPath(): String {
        var filesDir: File?
        filesDir = applicationContext.getExternalFilesDir(null)

        if (filesDir?.canWrite() != true || Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            if (BuildConfig.DEBUG) Log.d("TorrentServer", "Can't write to $filesDir or SDK>33")
            filesDir = null
        }

        if (filesDir == null) {
            filesDir = applicationContext.filesDir
            if (BuildConfig.DEBUG) Log.d("TorrentServer", "Use $filesDir for settings path")
        }
        if (filesDir == null) {
            filesDir = File(Environment.getExternalStorageDirectory().path, "TorrServe")
        }

        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }

        return filesDir.path
    }
}
