package eu.kanade.domain.episode.model

data class Episode(
    val id: Long,
    val animeId: Long,
    val seen: Boolean,
    val bookmark: Boolean,
    val lastSecondSeen: Long,
    val totalSeconds: Long,
    val dateFetch: Long,
    val sourceOrder: Long,
    val url: String,
    val name: String,
    val dateUpload: Long,
    val episodeNumber: Float,
    val scanlator: String?,
)
