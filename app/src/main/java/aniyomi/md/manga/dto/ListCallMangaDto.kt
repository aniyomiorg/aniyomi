package aniyomi.md.manga.dto

interface ListCallMangaDto<T> {
    val limit: Int
    val offset: Int
    val total: Int
    val data: List<T>
}
