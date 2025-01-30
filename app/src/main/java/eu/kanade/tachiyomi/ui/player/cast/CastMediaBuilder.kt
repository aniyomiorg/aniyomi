package eu.kanade.tachiyomi.ui.player.cast

import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.common.images.WebImage
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.PlayerViewModel

class CastMediaBuilder(private val viewModel: PlayerViewModel, private val activity: PlayerActivity) {
    private val player by lazy { activity.player }

    fun buildMediaInfo(): MediaInfo {
        val currentVideo = viewModel.videoList.value.getOrNull(viewModel.selectedVideoIndex.value)
            ?: throw IllegalStateException("Invalid video selection")

        return MediaInfo.Builder(currentVideo.videoUrl!!)
            .setContentType("video/mp4")
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .apply { addMetadata() }
            .apply { addTracks() }
            .setStreamDuration((player.duration ?: 0).toLong() * 1000)
            .build()
    }

    private fun MediaInfo.Builder.addMetadata(): MediaInfo.Builder {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, viewModel.currentAnime.value?.title ?: "")
            putString(MediaMetadata.KEY_SUBTITLE, viewModel.currentEpisode.value?.name ?: "")
            addImage(WebImage(Uri.parse(viewModel.currentAnime.value?.thumbnailUrl)))
        }
        return setMetadata(metadata)
    }

    private fun MediaInfo.Builder.addTracks(): MediaInfo.Builder {
        addSubtitlesToCast(this)
        buildAudioTracks(this)
        return this
    }

    private fun addSubtitlesToCast(mediaInfoBuilder: MediaInfo.Builder) {
        val subtitleTracks = viewModel.videoList.value
            .getOrNull(viewModel.selectedVideoIndex.value)
            ?.subtitleTracks
            ?.takeIf { it.isNotEmpty() }

        subtitleTracks?.let { subs ->
            val mediaTracks = subs.mapIndexed { index, sub ->
                MediaTrack.Builder(index.toLong(), MediaTrack.TYPE_TEXT)
                    .setContentId(sub.url)
                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                    .setName(sub.lang)
                    .build()
            }
            mediaInfoBuilder.setMediaTracks(mediaTracks)
        }
    }

    private fun buildAudioTracks(mediaInfoBuilder: MediaInfo.Builder) {
        val audioTracks = viewModel.videoList.value
            .getOrNull(viewModel.selectedVideoIndex.value)
            ?.audioTracks
            ?.takeIf { it.isNotEmpty() }

        audioTracks?.let { tracks ->
            val mediaTracks = tracks.mapIndexed { index, audio ->
                MediaTrack.Builder(index.toLong(), MediaTrack.TYPE_AUDIO)
                    .setContentId(audio.url)
                    .setName(audio.lang)
                    .build()
            }
            mediaInfoBuilder.setMediaTracks(mediaTracks)
        }
    }
}
