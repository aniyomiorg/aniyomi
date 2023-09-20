package tachiyomi.domain.source.anime.model

data class AnimeSourceData(
    val id: Long,
    val lang: String,
    val name: String,
) {

    val isMissingInfo: Boolean = name.isBlank() || lang.isBlank()
}
