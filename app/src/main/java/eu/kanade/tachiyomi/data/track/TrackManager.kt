package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.komga.Komga
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.simkl.Simkl

class TrackManager(context: Context) {

    companion object {
        const val MYANIMELIST = 1
        const val ANILIST = 2
        const val KITSU = 3
        const val SHIKIMORI = 4
        const val BANGUMI = 5
        const val KOMGA = 6
        const val MANGA_UPDATES = 7
        const val SIMKL = 101
    }

    val myAnimeList = MyAnimeList(context, MYANIMELIST)

    val aniList = Anilist(context, ANILIST)

    val kitsu = Kitsu(context, KITSU)

    val shikimori = Shikimori(context, SHIKIMORI)

    val simkl = Simkl(context, SIMKL)

    val bangumi = Bangumi(context, BANGUMI)

    val komga = Komga(context, KOMGA)

    val mangaUpdates = MangaUpdates(context, MANGA_UPDATES)

    val services = listOf(myAnimeList, aniList, kitsu, shikimori, bangumi, komga, mangaUpdates, simkl)

    fun getService(id: Int) = services.find { it.id == id }

    fun hasLoggedServices() = services.any { it.isLogged }

    fun hasLoggedMangaServices() = services.any { it.isLogged && it !is AnimeTrackService }

    fun hasLoggedAnimeServices() = services.any { it.isLogged && it !is MangaTrackService }
}
