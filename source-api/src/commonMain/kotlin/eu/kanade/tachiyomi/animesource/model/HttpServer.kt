package eu.kanade.tachiyomi.animesource.model

import fi.iki.elonen.NanoHTTPD
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

open class HttpServer : NanoHTTPD(0) {
    val url: String
        get() = "http://localhost:$listeningPort"

    fun isRunning(): Boolean {
        return isRunning
    }

    @Volatile
    private var isRunning = false

    override fun start() {
        try {
            super.start()
            isRunning = true
        } catch (e: Exception) {
            logcat(LogPriority.DEBUG, e) { "Failed to start http server" }
        }
    }

    override fun stop() {
        super.stop()
        isRunning = false
    }
}
