package eu.kanade.tachiyomi.ui.player.cast

import android.content.Intent
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.common.images.WebImage
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.torrentServer.TorrentServerApi
import eu.kanade.tachiyomi.torrentServer.TorrentServerUtils
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.util.LocalHttpServerHolder
import eu.kanade.tachiyomi.util.LocalHttpServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder

class CastMediaBuilder(
    private val viewModel: PlayerViewModel,
    private val activity: PlayerActivity,
) {

    private val player by lazy { activity.player }
    private val prefserver: LocalHttpServerHolder by injectLazy()
    private val port = prefserver.port().get()

    suspend fun buildMediaInfo(video: Video): MediaInfo = withContext(Dispatchers.IO) {
        var videoUrl = video.videoUrl
        logcat(LogPriority.DEBUG) { "Video URL: $videoUrl" }

        videoUrl = when {
            videoUrl.startsWith("content://") -> getLocalServerUrl(videoUrl)
            videoUrl.startsWith(
                "magnet",
            ) ||
                videoUrl.endsWith(".torrent") -> torrentLinkHandler(videoUrl, video.quality)
            else -> videoUrl
        }

        val contentType = when {
            videoUrl.contains(".m3u8") -> "application/x-mpegURL"
            videoUrl.contains(".mpd") -> "application/dash+xml"
            else -> "video/mp4"
        }

        MediaInfo.Builder(videoUrl)
            .setContentType(contentType)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .addMetadata(video)
            .addTracks(video)
            .setStreamDuration((player.duration ?: 0).toLong() * 1000)
            .build()
    }

    private fun torrentLinkHandler(videoUrl: String, quality: String): String {
        var index = 0

        if (videoUrl.startsWith("content://")) {
            val videoInputStream = activity.applicationContext.contentResolver.openInputStream(Uri.parse(videoUrl))
                ?: throw IllegalStateException("Unable to open InputStream for content: $videoUrl")
            val torrent = TorrentServerApi.uploadTorrent(videoInputStream, quality, "", "", false)
            return TorrentServerUtils.getTorrentPlayLink(torrent, 0)
        }

        if (videoUrl.startsWith("magnet") && videoUrl.contains("index=")) {
            index = try {
                videoUrl.substringAfter("index=").toInt()
            } catch (e: NumberFormatException) {
                0
            }
        }

        val currentTorrent = TorrentServerApi.addTorrent(videoUrl, quality, "", "", false)
        logcat(LogPriority.DEBUG) { "Torrent URL: $videoUrl" }
        return TorrentServerUtils.getTorrentPlayLink(currentTorrent, index)
    }

    private fun MediaInfo.Builder.addMetadata(video: Video): MediaInfo.Builder {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, viewModel.currentAnime.value?.title ?: "")
            putString(MediaMetadata.KEY_SUBTITLE, viewModel.currentEpisode.value?.name ?: "")
            viewModel.currentAnime.value?.thumbnailUrl?.let { url ->
                addImage(WebImage(Uri.parse(url)))
            }
        }
        return setMetadata(metadata)
    }

    private fun MediaInfo.Builder.addTracks(video: Video): MediaInfo.Builder {
        val subtitleTracks = video.subtitleTracks.mapIndexed { trackIndex, sub ->
            logcat(LogPriority.DEBUG) { "Subtitle URL: ${sub.url}" }
            MediaTrack.Builder(trackIndex.toLong(), MediaTrack.TYPE_TEXT)
                .setContentId(sub.url)
                .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                .setName(sub.lang)
                .build()
        }

        val audioTracks = video.audioTracks.mapIndexed { trackIndex, audio ->
            MediaTrack.Builder((subtitleTracks.size + trackIndex).toLong(), MediaTrack.TYPE_AUDIO)
                .setContentId(audio.url)
                .setName(audio.lang)
                .setContentType("application/x-mpegURL")
                .build()
        }

        return setMediaTracks(subtitleTracks + audioTracks)
    }

    private fun getLocalServerUrl(contentUri: String): String {
        val context = activity.applicationContext
        context.startService(Intent(context, LocalHttpServerService::class.java))
        val ip = getLocalIpAddress()
        val encodedUri = URLEncoder.encode(contentUri, "UTF-8")
        return "http://$ip:$port/file?uri=$encodedUri"
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (ex: Exception) {
            logcat(LogPriority.DEBUG) { "Error getting local IP address" }
        }
        return "127.0.0.1"
    }
}
