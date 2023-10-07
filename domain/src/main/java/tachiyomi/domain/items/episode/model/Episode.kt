package tachiyomi.domain.items.episode.model

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
) {
    val isRecognizedNumber: Boolean
        get() = episodeNumber >= 0f

    companion object {
        fun create() = Episode(
            id = -1,
            animeId = -1,
            seen = false,
            bookmark = false,
            lastSecondSeen = 0,
            totalSeconds = 0,
            dateFetch = 0,
            sourceOrder = 0,
            url = "",
            name = "",
            dateUpload = -1,
            episodeNumber = -1f,
            scanlator = null,
        )
    }
}
