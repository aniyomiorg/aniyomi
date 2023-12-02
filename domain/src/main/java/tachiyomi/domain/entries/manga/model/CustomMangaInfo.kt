package tachiyomi.domain.entries.manga.model

data class CustomMangaInfo(
    val id: Long,
    val title: String?,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genre: List<String>? = null,
    val status: Long? = null,
)
