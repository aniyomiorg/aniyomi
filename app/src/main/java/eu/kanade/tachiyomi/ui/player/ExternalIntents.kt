package eu.kanade.tachiyomi.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.injectLazy

class ExternalIntents(val anime: Anime, val source: AnimeSource) {
    private val preferences: PreferencesHelper by injectLazy()

    fun getExternalIntent(episode: Episode, video: Video, context: Context): Intent? {
        val videoUrl: Uri
        if (video.videoUrl == null) {
            makeErrorToast(context, Exception("video URL is null."))
            return null
        } else {
            videoUrl = Uri.parse(video.videoUrl)
        }
        val pkgName = preferences.externalPlayerPreference()
        val anime = anime
        return if (pkgName.isNullOrEmpty()) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndTypeAndNormalize(videoUrl, getMime(videoUrl))
                putExtra("title", anime.title + " - " + episode.name)
                putExtra("position", episode.last_second_seen.toInt())
                putExtra("return_result", true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val headers = video.headers ?: (source as? AnimeHttpSource)?.headers
                if (headers != null) {
                    var headersArray = arrayOf<String>()
                    for (header in headers) {
                        headersArray += arrayOf(header.first, header.second)
                    }
                    val headersString = headersArray.drop(2).joinToString(": ")
                    putExtra("headers", headersArray)
                    putExtra("http-header-fields", headersString)
                }
            }
        } else standardIntentForPackage(pkgName, context, videoUrl, episode, video)
    }

    private fun makeErrorToast(context: Context, e: Exception?) {
        launchUI { context.toast(e?.message ?: "Cannot open episode") }
    }

    private fun standardIntentForPackage(pkgName: String, context: Context, uri: Uri, episode: Episode, video: Video): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            if (isPackageInstalled(pkgName, context.packageManager)) {
                component = getComponent(pkgName)
            }
            setDataAndType(uri, "video/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra("title", episode.name)
            putExtra("position", episode.last_second_seen.toInt())
            putExtra("return_result", true)
            putExtra("secure_uri", true)

            /*val externalSubs = source.getExternalSubtitleStreams()
            val enabledSubUrl = when {
                source.selectedSubtitleStream != null -> {
                    externalSubs.find { stream -> stream.index == source.selectedSubtitleStream?.index }?.let { sub ->
                        apiClient.createUrl(sub.deliveryUrl)
                    }
                }
                else -> null
            }

            // MX Player API / MPV
            putExtra("subs", externalSubs.map { stream -> Uri.parse(apiClient.createUrl(stream.deliveryUrl)) }.toTypedArray())
            putExtra("subs.name", externalSubs.map(ExternalSubtitleStream::displayTitle).toTypedArray())
            putExtra("subs.filename", externalSubs.map(ExternalSubtitleStream::language).toTypedArray())
            putExtra("subs.enable", enabledSubUrl?.let { url -> arrayOf(Uri.parse(url)) } ?: emptyArray())

            // VLC
            if (enabledSubUrl != null) putExtra("subtitles_location", enabledSubUrl)*/

            // headers
            val headers = video.headers ?: (source as? AnimeHttpSource)?.headers
            if (headers != null) {
                var headersArray = arrayOf<String>()
                for (header in headers) {
                    headersArray += arrayOf(header.first, header.second)
                }
                putExtra("headers", headersArray)
            }
        }
    }

    private fun getMime(uri: Uri): String {
        return when (uri.path?.substringAfterLast(".")) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "m3u8" -> "application/x-mpegURL"
            else -> "video/any"
        }
    }

    /**
     * To ensure that the correct activity is called.
     */
    private fun getComponent(packageName: String): ComponentName? {
        return when (packageName) {
            MPV_PLAYER -> ComponentName(packageName, "$packageName.MPVActivity")
            MX_PLAYER_FREE, MX_PLAYER_PRO -> ComponentName(packageName, "$packageName.ActivityScreen")
            VLC_PLAYER -> ComponentName(packageName, "$packageName.gui.video.VideoPlayerActivity")
            MPV_REMOTE -> ComponentName(packageName, "$packageName.MainActivity")
            else -> null
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

private const val MPV_PLAYER = "is.xyz.mpv"
private const val MX_PLAYER_FREE = "com.mxtech.videoplayer.ad"
private const val MX_PLAYER_PRO = "com.mxtech.videoplayer.pro"
private const val VLC_PLAYER = "org.videolan.vlc"
private const val MPV_REMOTE = "com.husudosu.mpvremote"
