package eu.kanade.data.source.manga

import androidx.paging.PagingState
import eu.kanade.data.items.chapter.NoChaptersException
import eu.kanade.domain.source.manga.model.SourcePagingSourceType
import eu.kanade.tachiyomi.source.CatalogueMangaSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.withIOContext

abstract class SourcePagingSource(
    protected val source: CatalogueMangaSource,
) : SourcePagingSourceType() {

    abstract suspend fun requestNextPage(currentPage: Int): MangasPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SManga> {
        val page = params.key ?: 1

        val mangasPage = try {
            withIOContext {
                requestNextPage(page.toInt())
                    .takeIf { it.mangas.isNotEmpty() }
                    ?: throw NoChaptersException()
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = mangasPage.mangas,
            prevKey = null,
            nextKey = if (mangasPage.hasNextPage) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Long, SManga>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}

class SourceSearchPagingSource(source: CatalogueMangaSource, val query: String, val filters: FilterList) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.fetchSearchManga(currentPage, query, filters).awaitSingle()
    }
}

class SourcePopularPagingSource(source: CatalogueMangaSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.fetchPopularManga(currentPage).awaitSingle()
    }
}

class SourceLatestPagingSource(source: CatalogueMangaSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.fetchLatestUpdates(currentPage).awaitSingle()
    }
}
