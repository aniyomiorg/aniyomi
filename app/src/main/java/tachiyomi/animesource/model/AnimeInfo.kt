package tachiyomi.animesource.model

/**
 * Model for a anime given by a source
 *
 * TODO: we should avoid data class due to possible incompatibilities across versions
 */
data class AnimeInfo(
    val key: String,
    val title: String,
    val artist: String = "",
    val author: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val status: Int = UNKNOWN,
    val cover: String = "",
) {

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
    }
}
