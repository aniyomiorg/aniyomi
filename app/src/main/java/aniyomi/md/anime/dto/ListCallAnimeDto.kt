package aniyomi.md.anime.dto

interface ListCallAnimeDto<T> {
    val limit: Int
    val offset: Int
    val total: Int
    val data: List<T>
}
