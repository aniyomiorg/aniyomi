package eu.kanade.tachiyomi.util

import android.content.Context
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MpvVersionsUtil(context: Context) : MPVLib.LogObserver {

    val mpvVersions = MPVVersions()

    private var recordMPVLog = true

    init {
        MPVLib.create(context, "v")
        MPVLib.addLogObserver(this)
        MPVLib.init()
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (prefix != "cplayer") return

        if (level == MPVLib.mpvLogLevel.MPV_LOG_LEVEL_V) {
            with(text) {
                if (recordMPVLog) {
                    when {
                        contains("Copyright ©") -> mpvVersions.mpvCommit = this
                        contains("built on") -> mpvVersions.buildDate = this
                        contains("libplacebo version:") -> mpvVersions.libPlacebo = this
                        contains("FFmpeg version:") -> mpvVersions.ffmpeg = this
                        else -> {
                            recordMPVLog = false
                            runBlocking { onLoggingComplete() }
                        }
                    }
                }
            }
        }
    }

    private suspend fun onLoggingComplete() {
        withContext(Dispatchers.IO) {
            mpvVersions.trim()
            MPVLib.removeLogObserver(this@MpvVersionsUtil)
            MPVLib.destroy()
        }
    }

    data class MPVVersions(
        var mpvCommit: String = "",
        var buildDate: String = "",
        var libPlacebo: String = "",
        var ffmpeg: String = "",
    ) {
        fun trim() {
            mpvCommit = mpvCommit.substringBefore("Copyright").trim()
            buildDate = buildDate.substringAfter("built on ").trim()
            libPlacebo = libPlacebo.substringAfter(": ").trim()
            ffmpeg = ffmpeg.substringAfter(": ").trim()
        }
    }
}
