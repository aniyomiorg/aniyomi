package tachiyomi.domain.source.anime.model

data class AnimeSourceWithCount(
    val source: AnimeSource,
    val count: Long,
) {

    val id: Long
        get() = source.id

    val name: String
        get() = source.name
}
