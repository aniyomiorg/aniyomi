package tachiyomi.domain.source.anime.model

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video

@Suppress("OverridingDeprecatedMember")
class StubAnimeSource(
    override val id: Long,
    override val lang: String,
    override val name: String,
) : AnimeSource {

    private val isInvalid: Boolean = name.isBlank() || lang.isBlank()

    override val supportsLatest: Boolean = false

    override suspend fun getPopularAnime(page: Int): AnimesPage =
        throw AnimeSourceNotInstalledException()

    override suspend fun getLatestUpdates(page: Int): AnimesPage =
        throw AnimeSourceNotInstalledException()

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage =
        throw AnimeSourceNotInstalledException()

    override suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate = throw AnimeSourceNotInstalledException()

    override suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate = throw AnimeSourceNotInstalledException()

    override suspend fun getHosterList(episode: SEpisode): List<Hoster> =
        throw AnimeSourceNotInstalledException()

    override suspend fun getVideoList(hoster: Hoster): List<Video> =
        throw AnimeSourceNotInstalledException()

    override fun toString(): String =
        if (!isInvalid) "$name (${lang.uppercase()})" else id.toString()

    companion object {
        fun from(source: AnimeSource): StubAnimeSource {
            return StubAnimeSource(id = source.id, lang = source.lang, name = source.name)
        }
    }
}
class AnimeSourceNotInstalledException : Exception()
