package eu.kanade.tachiyomi.animesource.model

class AnimesPage(val animes: List<SAnime>, val hasNextPage: Boolean) {

    @Deprecated("AnimesPage is now a regular class")
    operator fun component1(): List<SAnime> = animes

    @Deprecated("AnimesPage is now a regular class")
    operator fun component2(): Boolean = hasNextPage

    @Deprecated("AnimesPage is now a regular class")
    fun copy(
        animes: List<SAnime> = this.animes,
        hasNextPage: Boolean = this.hasNextPage,
    ): AnimesPage = AnimesPage(
        animes = animes,
        hasNextPage = hasNextPage,
    )
}
