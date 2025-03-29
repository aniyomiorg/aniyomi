package tachiyomi.domain.items.episode.model

data class EpisodeUpdate(
    val id: Long,
    val animeId: Long? = null,
    val seen: Boolean? = null,
    val bookmark: Boolean? = null,
    val fillermark: Boolean? = null,
    val lastSecondSeen: Long? = null,
    val totalSeconds: Long? = null,
    val dateFetch: Long? = null,
    val sourceOrder: Long? = null,
    val url: String? = null,
    val name: String? = null,
    val dateUpload: Long? = null,
    val episodeNumber: Double? = null,
    val scanlator: String? = null,
    val summary: String? = null,
    val previewUrl: String? = null,
    val version: Long? = null,
)

fun Episode.toEpisodeUpdate(): EpisodeUpdate {
    return EpisodeUpdate(
        id,
        animeId,
        seen,
        bookmark,
        fillermark,
        lastSecondSeen,
        totalSeconds,
        dateFetch,
        sourceOrder,
        url,
        name,
        dateUpload,
        episodeNumber,
        scanlator,
        summary,
        previewUrl,
        version,
    )
}
