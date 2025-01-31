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

    fun buildMediaInfo(index: Int): MediaInfo {
        val video = viewModel.videoList.value.getOrNull(index)
            ?: throw IllegalStateException("Invalid video index: $index")

        return MediaInfo.Builder(video.videoUrl!!)
            .setContentType("video/mp4")
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
            addImage(WebImage(Uri.parse(viewModel.currentAnime.value?.thumbnailUrl)))
        }
        return setMetadata(metadata)
    }

    private fun addSubtitlesToCast(index: Int): List<MediaTrack> {
        val subtitleTracks = viewModel.videoList.value[index].subtitleTracks

        return subtitleTracks.mapIndexed { trackIndex, sub ->
            MediaTrack.Builder(trackIndex.toLong(), MediaTrack.TYPE_TEXT)
                .setContentId(sub.url)
                .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                .setName(sub.lang)
                .build()
        }
    }

    private fun buildAudioTracks(index: Int): List<MediaTrack> {
        val audioTracks = viewModel.videoList.value[index].audioTracks

        return audioTracks.mapIndexed { trackIndex, audio ->
            MediaTrack.Builder(trackIndex.toLong(), MediaTrack.TYPE_AUDIO)
                .setContentId(audio.url)
                .setName(audio.lang)
                .build()
        }
    }

    private fun MediaInfo.Builder.addTracks(index: Int): MediaInfo.Builder {
        val subtitleTracks = addSubtitlesToCast(index)
        val audioTracks = buildAudioTracks(index)
        return setMediaTracks(subtitleTracks + audioTracks)
    }
}
