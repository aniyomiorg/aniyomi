package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import eu.kanade.tachiyomi.data.database.resolvers.AnimeFlagsPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimelibAnimeGetResolver
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable
import eu.kanade.tachiyomi.data.database.tables.AnimeTable
import eu.kanade.tachiyomi.data.database.tables.CategoryTable
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable

interface AnimeQueries : DbProvider {

    fun getAnimelibAnimes() = db.get()
        .listOfObjects(AnimelibAnime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(animelibQuery)
                .observesTables(AnimeTable.TABLE, EpisodeTable.TABLE, AnimeCategoryTable.TABLE, CategoryTable.TABLE)
                .build(),
        )
        .withGetResolver(AnimelibAnimeGetResolver.INSTANCE)
        .prepare()

    fun getFavoriteAnimes() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTable.TABLE)
                .where("${AnimeTable.COL_FAVORITE} = ?")
                .whereArgs(1)
                .build(),
        )
        .prepare()

    fun getAnime(url: String, sourceId: Long) = db.get()
        .`object`(Anime::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTable.TABLE)
                .where("${AnimeTable.COL_URL} = ? AND ${AnimeTable.COL_SOURCE} = ?")
                .whereArgs(url, sourceId)
                .build(),
        )
        .prepare()

    fun getAnime(id: Long) = db.get()
        .`object`(Anime::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTable.TABLE)
                .where("${AnimeTable.COL_ID} = ?")
                .whereArgs(id)
                .build(),
        )
        .prepare()

    fun insertAnime(anime: Anime) = db.put().`object`(anime).prepare()

    fun insertAnimes(animes: List<Anime>) = db.put().objects(animes).prepare()

    fun updateEpisodeFlags(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeFlagsPutResolver(AnimeTable.COL_EPISODE_FLAGS, Anime::episode_flags))
        .prepare()

    fun updateEpisodeFlags(animes: List<Anime>) = db.put()
        .objects(animes)
        .withPutResolver(AnimeFlagsPutResolver(AnimeTable.COL_EPISODE_FLAGS, Anime::episode_flags))
        .prepare()

    fun updateViewerFlags(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeFlagsPutResolver(AnimeTable.COL_VIEWER, Anime::viewer_flags))
        .prepare()

    fun getLastSeenAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLastSeenAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build(),
        )
        .prepare()

    fun getLatestEpisodeAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLatestEpisodeAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build(),
        )
        .prepare()

    fun getEpisodeFetchDateAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getEpisodeFetchDateAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build(),
        )
        .prepare()
}
