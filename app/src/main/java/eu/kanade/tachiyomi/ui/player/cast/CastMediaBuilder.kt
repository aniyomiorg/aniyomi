package eu.kanade.tachiyomi.ui.player.cast

import android.content.Intent
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.common.images.WebImage
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.util.LocalHttpServerHolder
import eu.kanade.tachiyomi.util.LocalHttpServerService
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder

class CastMediaBuilder(private val viewModel: PlayerViewModel, private val activity: PlayerActivity) {
    private val player by lazy { activity.player }

    fun buildMediaInfo(index: Int): MediaInfo {
        val video = viewModel.videoList.value.getOrNull(index)
            ?: throw IllegalStateException("Invalid video index: $index")

        // Obtener la URL original
        var videoUrl = video.videoUrl ?: throw IllegalStateException("Video URL is null")
        logcat(LogPriority.DEBUG) { "Video URL: $videoUrl" }

        // Si es un URI local, convertirlo a URL accesible vía HTTP
        if (videoUrl.startsWith("content://")) {
            videoUrl = getLocalServerUrl(videoUrl)
            logcat(LogPriority.DEBUG) { "Local Server URL: $videoUrl" }
        }

        val contentType = when {
            videoUrl.contains(".m3u8") -> "application/x-mpegURL"
            videoUrl.contains(".mpd") -> "application/dash+xml"
            else -> "video/mp4"
        }

        return MediaInfo.Builder(videoUrl)
            .setContentType(contentType)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .apply { addMetadata() }
            .apply { addTracks(index) }
            .setStreamDuration((player.duration ?: 0).toLong() * 1000)
            .build()
    }

    private fun MediaInfo.Builder.addMetadata(): MediaInfo.Builder {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, viewModel.currentAnime.value?.title ?: "")
            putString(MediaMetadata.KEY_SUBTITLE, viewModel.currentEpisode.value?.name ?: "")
            viewModel.currentAnime.value?.thumbnailUrl?.let {
                addImage(WebImage(Uri.parse(it)))
            }
        }
        return setMetadata(metadata)
    }

    private fun addSubtitlesToCast(index: Int): List<MediaTrack> {
        return viewModel.videoList.value[index].subtitleTracks.mapIndexed { trackIndex, sub ->
            MediaTrack.Builder(trackIndex.toLong(), MediaTrack.TYPE_TEXT)
                .setContentId(sub.url)
                .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                .setName(sub.lang)
                .build()
        }
    }

    private fun MediaInfo.Builder.addTracks(index: Int): MediaInfo.Builder {
        val subtitleTracks = addSubtitlesToCast(index)
        val subtitleCount = subtitleTracks.size
        val audioTracks = buildAudioTracks(index, subtitleCount)
        return setMediaTracks(subtitleTracks + audioTracks)
    }

    private fun buildAudioTracks(index: Int, idOffset: Int): List<MediaTrack> {
        return viewModel.videoList.value[index].audioTracks.mapIndexed { trackIndex, audio ->
            MediaTrack.Builder((idOffset + trackIndex).toLong(), MediaTrack.TYPE_AUDIO)
                .setContentId(audio.url)
                .setName(audio.lang)
                .setContentType("application/x-mpegURL")
                .build()
        }
    }

    /**
     * If a local URI (content://) is detected, the local server is started (if not already started)
     * and an accessible HTTP URL for the Cast device is constructed.
     */
    private fun getLocalServerUrl(contentUri: String): String {
        val context = activity.applicationContext
        context.startService(Intent(context, LocalHttpServerService::class.java))
        val ip = getLocalIpAddress()
        val encodedUri = URLEncoder.encode(contentUri, "UTF-8")
        return "http://$ip:${LocalHttpServerHolder.PORT}/file?uri=$encodedUri"
    }

    /**
     * Gets the local IP address (IPv4) of the device.
     * Make sure that the network allows the connection from the Cast device.
     */

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
