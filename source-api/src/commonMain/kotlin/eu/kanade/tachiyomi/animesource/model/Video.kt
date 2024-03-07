package eu.kanade.tachiyomi.animesource.model

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Headers

@Serializable
data class Track(val url: String, val lang: String)

open class Video(
    val url: String = "",
    val quality: String = "",
    var videoUrl: String? = null,
    val headers: Headers? = null,
    // "url", "language-label-2", "url2", "language-label-2"
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
) {

    @Suppress("UNUSED_PARAMETER")
    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        uri: Uri? = null,
        headers: Headers? = null,
    ) : this(url, quality, videoUrl, headers)

    @Transient
    @Volatile
    var status: State = State.QUEUE
        set(value) {
            field = value
        }

    enum class State {
        QUEUE,
        LOAD_VIDEO,
        READY,
        ERROR,
    }
}

@Serializable
data class SerializableVideo(
    val url: String = "",
    val quality: String = "",
    var videoUrl: String? = null,
    val headers: List<Pair<String, String>>? = null,
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
) {

    companion object {
        fun List<Video>.serialize(): String =
            Json.encodeToString(
                this.map { vid ->
                    SerializableVideo(
                        vid.url,
                        vid.quality,
                        vid.videoUrl,
                        headers = vid.headers?.toList(),
                        vid.subtitleTracks,
                        vid.audioTracks,
                    )
                },
            )

        fun String.toVideoList(): List<Video> =
            Json.decodeFromString<List<SerializableVideo>>(this)
                .map { sVid ->
                    Video(
                        sVid.url,
                        sVid.quality,
                        sVid.videoUrl,
                        sVid.headers
                            ?.flatMap { it.toList() }
                            ?.let { Headers.headersOf(*it.toTypedArray()) },
                        sVid.subtitleTracks,
                        sVid.audioTracks,
                    )
                }
    }
}
