package tachiyomi.domain.source.anime.model

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video

@Suppress("OverridingDeprecatedMember")
class StubAnimeSource(
    override val id: Long,
    override val name: String,
    override val lang: String,
) : AnimeSource {

    val isInvalid: Boolean = name.isBlank() || lang.isBlank()

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        throw AnimeSourceNotInstalledException()
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        throw AnimeSourceNotInstalledException()
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        throw AnimeSourceNotInstalledException()
    }

    override fun toString(): String {
        return if (isInvalid.not()) "$name (${lang.uppercase()})" else id.toString()
    }
}
class AnimeSourceNotInstalledException : Exception()
