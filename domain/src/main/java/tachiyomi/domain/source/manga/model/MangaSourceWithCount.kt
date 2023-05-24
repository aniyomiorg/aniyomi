package tachiyomi.domain.source.manga.model

data class MangaSourceWithCount(
    val source: Source,
    val count: Long,
) {

    val id: Long
        get() = source.id

    val name: String
        get() = source.name
}
