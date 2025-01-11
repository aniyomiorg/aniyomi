package eu.kanade.tachiyomi.data.track.model

data class TrackMangaMetadata(
    val remoteId: Long? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val authors: String? = null,
    val artists: String? = null,
)
