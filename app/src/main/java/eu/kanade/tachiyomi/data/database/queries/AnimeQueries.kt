package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.Queries
import com.pushtorefresh.storio.sqlite.operations.get.PreparedGetListOfObjects
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import eu.kanade.tachiyomi.data.database.models.SourceIdAnimeCount
import eu.kanade.tachiyomi.data.database.resolvers.AnimeCoverLastModifiedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeFavoritePutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeFlagsPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeLastUpdatedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeTitlePutResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimelibAnimeGetResolver
import eu.kanade.tachiyomi.data.database.resolvers.SourceIdAnimeCountGetResolver
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
                .build()
        )
        .withGetResolver(AnimelibAnimeGetResolver.INSTANCE)
        .prepare()

    fun getFavoriteAnimes(sortByTitle: Boolean = true): PreparedGetListOfObjects<Anime> {
        var queryBuilder = Query.builder()
            .table(AnimeTable.TABLE)
            .where("${AnimeTable.COL_FAVORITE} = ?")
            .whereArgs(1)

        if (sortByTitle) {
            queryBuilder = queryBuilder.orderBy(AnimeTable.COL_TITLE)
        }

        return db.get()
            .listOfObjects(Anime::class.java)
            .withQuery(queryBuilder.build())
            .prepare()
    }

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

    fun getSourceIdsWithNonLibraryAnime() = db.get()
        .listOfObjects(SourceIdAnimeCount::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getSourceIdsWithNonLibraryAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .withGetResolver(SourceIdAnimeCountGetResolver.INSTANCE)
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

    fun updateViewerFlags(anime: List<Anime>) = db.put()
        .objects(anime)
        .withPutResolver(AnimeFlagsPutResolver(AnimeTable.COL_VIEWER, Anime::viewer_flags))
        .prepare()

    fun updateLastUpdated(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeLastUpdatedPutResolver())
        .prepare()

    fun updateAnimeFavorite(anime: Anime) = db.put()
        .`object`(anime)
        .withPutResolver(AnimeFavoritePutResolver())
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

    fun deleteAnimesNotInLibraryBySourceIds(sourceIds: List<Long>) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(AnimeTable.TABLE)
                .where("${AnimeTable.COL_FAVORITE} = ? AND ${AnimeTable.COL_SOURCE} IN (${Queries.placeholders(sourceIds.size)})")
                .whereArgs(0, *sourceIds.toTypedArray())
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

    fun getLastSeenAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLastSeenAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getTotalEpisodeAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getTotalEpisodeAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getLatestEpisodeAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLatestEpisodeAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()

    fun getEpisodeFetchDateAnime() = db.get()
        .listOfObjects(Anime::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getEpisodeFetchDateAnimeQuery())
                .observesTables(AnimeTable.TABLE)
                .build()
        )
        .prepare()
}
