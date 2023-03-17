package eu.kanade.domain.source.manga.model

data class MangaSourceData(
    val id: Long,
    val lang: String,
    val name: String,
) {

    val isMissingInfo: Boolean = name.isBlank() || lang.isBlank()
}
