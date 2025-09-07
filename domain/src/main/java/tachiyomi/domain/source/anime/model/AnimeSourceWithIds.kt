package tachiyomi.domain.source.anime.model

data class AnimeSourceWithIds(
    val source: AnimeSource,
    val ids: List<Long>,
    val orphaned: List<Long>,
) {
    val count: Long
        get() = ids.size.toLong()

    val id: Long
        get() = source.id

    val name: String
        get() = source.name
}
