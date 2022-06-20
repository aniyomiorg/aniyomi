package eu.kanade.tachiyomi.animesource

import android.graphics.drawable.Drawable
import eu.kanade.domain.animesource.model.AnimeSourceData
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.model.toAnimeInfo
import eu.kanade.tachiyomi.animesource.model.toEpisodeInfo
import eu.kanade.tachiyomi.animesource.model.toSAnime
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.animesource.model.toVideoUrl
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.util.lang.awaitSingle
import rx.Observable
import tachiyomi.animesource.model.AnimeInfo
import tachiyomi.animesource.model.EpisodeInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface AnimeSource : tachiyomi.animesource.AnimeSource {

    /**
     * Id for the source. Must be unique.
     */
    override val id: Long

    /**
     * Name of the source.
     */
    override val name: String

    override val lang: String
        get() = ""

    /**
     * Returns an observable with the updated details for a anime.
     *
     * @param anime the anime to update.
     */
    @Deprecated(
        "Use the 1.x API instead",
        ReplaceWith("getAnimeDetails"),
    )
    fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> = throw IllegalStateException("Not used")

    /**
     * Returns an observable with all the available episodes for an anime.
     *
     * @param anime the anime to update.
     */
    @Deprecated(
        "Use the 1.x API instead",
        ReplaceWith("getEpisodeList"),
    )
    fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> = throw IllegalStateException("Not used")

    /**
     * Returns an observable with a list of video for the episode of an anime.
     *
     * @param episode the episode to get the link for.
     */
    @Deprecated(
        "Use the 1.x API instead",
        ReplaceWith("getVideoList"),
    )
    fun fetchVideoList(episode: SEpisode): Observable<List<Video>> = Observable.empty()

    /**
     * [1.x API] Get the updated details for a anime.
     */
    @Suppress("DEPRECATION")
    override suspend fun getAnimeDetails(anime: AnimeInfo): AnimeInfo {
        val sAnime = anime.toSAnime()
        val networkAnime = fetchAnimeDetails(sAnime).awaitSingle()
        sAnime.copyFrom(networkAnime)
        return sAnime.toAnimeInfo()
    }

    /**
     * [1.x API] Get all the available episodes for a anime.
     */
    @Suppress("DEPRECATION")
    override suspend fun getEpisodeList(anime: AnimeInfo): List<EpisodeInfo> {
        return fetchEpisodeList(anime.toSAnime()).awaitSingle()
            .map { it.toEpisodeInfo() }
    }

    /**
     * [1.x API] Get a link for the episode of an anime.
     */
    @Suppress("DEPRECATION")
    override suspend fun getVideoList(episode: EpisodeInfo): List<tachiyomi.animesource.model.Video> {
        return fetchVideoList(episode.toSEpisode()).awaitSingle()
            .map { it.toVideoUrl() }
    }
}

fun AnimeSource.icon(): Drawable? = Injekt.get<AnimeExtensionManager>().getAppIconForSource(this)

fun AnimeSource.getPreferenceKey(): String = "source_$id"

fun AnimeSource.toAnimeSourceData(): AnimeSourceData = AnimeSourceData(id = id, lang = lang, name = name)

fun AnimeSource.getNameForAnimeInfo(): String {
    val preferences = Injekt.get<PreferencesHelper>()
    val enabledLanguages = preferences.enabledLanguages().get()
        .filterNot { it in listOf("all", "other") }
    val hasOneActiveLanguages = enabledLanguages.size == 1
    val isInEnabledLanguages = lang in enabledLanguages
    return when {
        // For edge cases where user disables a source they got manga of in their library.
        hasOneActiveLanguages && !isInEnabledLanguages -> toString()
        // Hide the language tag when only one language is used.
        hasOneActiveLanguages && isInEnabledLanguages -> name
        else -> toString()
    }
}
