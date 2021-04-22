package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import eu.kanade.tachiyomi.data.database.resolvers.AnimeCoverLastModifiedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeFavoritePutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeFlagsPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeLastUpdatedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeTitlePutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeViewerPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimelibAnimeGetResolver
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable
import eu.kanade.tachiyomi.data.database.tables.AnimeTable
import eu.kanade.tachiyomi.data.database.tables.CategoryTable
import eu.kanade.tachiyomi.data.database.tables.ChapterTable

interface AnimeQueries : DbProvider {

    fun getAnimes() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getAnimelibAnimes() = db.get()
        .listOfObjects(AnimelibAnime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(libraryQuery)
                .observesTables(AnimeTable.TABLE, ChapterTable.TABLE, AnimeCategoryTable.TABLE, CategoryTable.TABLE)
                .build()
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
                .orderBy(AnimeTable.COL_TITLE)
                .build()
        )
        .prepare()

    fun getAnime(url: String, sourceId: Long) = db.get()
        .`object`(Anime::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTable.TABLE)
                .where("${AnimeTable.COL_URL} = ? AND ${AnimeTable.COL_SOURCE} = ?")
                .whereArgs(url, sourceId)
                .build()
        )
        .prepare()

    fun getAnime(id: Long) = db.get()
        .`object`(Anime::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTable.TABLE)
                .where("${AnimeTable.COL_ID} = ?")
                .whereArgs(id)
                .build()
        )
        .prepare()

    fun insertAnime(anime: Anime) = db.put().`object`(anime).prepare()

    fun insertAnimes(animes: List<Anime>) = db.put().objects(animes).prepare()

    fun updateFlags(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeFlagsPutResolver())
        .prepare()

    fun updateFlags(animes: List<Anime>) = db.put()
        .objects(animes)
        .withPutResolver(AnimeFlagsPutResolver(true))
        .prepare()

    fun updateLastUpdated(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeLastUpdatedPutResolver())
        .prepare()

    fun updateAnimeFavorite(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeFavoritePutResolver())
        .prepare()

    fun updateAnimeViewer(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeViewerPutResolver())
        .prepare()

    fun updateAnimeTitle(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeTitlePutResolver())
        .prepare()

    fun updateAnimeCoverLastModified(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeCoverLastModifiedPutResolver())
        .prepare()

    fun deleteAnime(anime: Anime) = db.delete().`object`(anime).prepare()

    fun deleteAnimes(animes: List<Anime>) = db.delete().objects(animes).prepare()

    fun deleteAnimesNotInAnimelib() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(AnimeTable.TABLE)
                .where("${AnimeTable.COL_FAVORITE} = ?")
                .whereArgs(0)
                .build()
        )
        .prepare()

    fun deleteAnimes() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getLastReadAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLastReadAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getTotalChapterAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getTotalChapterAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getLatestChapterAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLatestChapterAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getChapterFetchDateAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getChapterFetchDateAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()
}
