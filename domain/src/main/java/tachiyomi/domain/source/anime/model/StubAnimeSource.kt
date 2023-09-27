package tachiyomi.domain.source.anime.model

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video

@Suppress("OverridingDeprecatedMember")
class StubAnimeSource(private val sourceData: AnimeSourceData) : AnimeSource {

    override val id: Long = sourceData.id

    override val name: String = sourceData.name.ifBlank { id.toString() }

    override val lang: String = sourceData.lang

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
        return if (sourceData.isMissingInfo.not()) "$name (${lang.uppercase()})" else id.toString()
    }
}
class AnimeSourceNotInstalledException : Exception()
