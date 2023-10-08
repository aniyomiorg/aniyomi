package eu.kanade.tachiyomi.ui.player

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.util.system.toast
import `is`.xyz.mpv.MPVLib
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchUI
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat

class PlayerObserver(val activity: PlayerActivity) :
    MPVLib.EventObserver,
    MPVLib.LogObserver {

    override fun eventProperty(property: String) {
        activity.runOnUiThread { activity.eventPropertyUi(property) }
    }

    override fun eventProperty(property: String, value: Boolean) {
        activity.runOnUiThread { activity.eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: Long) {
        activity.runOnUiThread { activity.eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: String) {}

    override fun event(eventId: Int) {
        when (eventId) {
            MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> activity.viewModel.viewModelScope.launchIO { activity.fileLoaded() }
            MPVLib.mpvEventId.MPV_EVENT_START_FILE -> activity.viewModel.viewModelScope.launchUI {
                activity.player.paused = false
                activity.refreshUi()
                // Fixes a minor Ui bug but I have no idea why
                val isEpisodeOnline = withIOContext { activity.viewModel.isEpisodeOnline() != true }
                if (isEpisodeOnline) activity.showLoadingIndicator(false)
            }
        }
    }

    override fun efEvent(err: String?) {
        var errorMessage = err ?: "Error: File ended"
        if (!httpError.isNullOrEmpty()) {
            errorMessage += ": $httpError"
            httpError = null
        }
        logcat(LogPriority.ERROR) { errorMessage }
        activity.runOnUiThread {
            activity.showLoadingIndicator(false)
            activity.toast(errorMessage, Toast.LENGTH_LONG)
        }
    }

    private var httpError: String? = null

    override fun logMessage(prefix: String, level: Int, text: String) {
        val logPriority = when (level) {
            MPVLib.mpvLogLevel.MPV_LOG_LEVEL_FATAL, MPVLib.mpvLogLevel.MPV_LOG_LEVEL_ERROR -> LogPriority.ERROR
            MPVLib.mpvLogLevel.MPV_LOG_LEVEL_WARN -> LogPriority.WARN
            MPVLib.mpvLogLevel.MPV_LOG_LEVEL_INFO -> LogPriority.INFO
            else -> null
        }
        if (logPriority != null) {
            if (text.contains("HTTP error")) httpError = text
            logcat.logcat("mpv/$prefix", logPriority) { text }
        }
    }
}
