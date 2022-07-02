package eu.kanade.data.category

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.category.model.CategoryUpdate
import eu.kanade.domain.category.repository.CategoryRepositoryAnime
import eu.kanade.domain.category.repository.DuplicateNameException
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImplAnime(
    private val handler: AnimeDatabaseHandler,
) : CategoryRepositoryAnime {

    override fun getAll(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getCategories(categoryMapper) }
    }

    override suspend fun getCategoriesByAnimeId(animeId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategoriesByAnimeId(animeId, categoryMapper)
        }
    }

    override fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getCategoriesByAnimeId(animeId, categoryMapper)
        }
    }

    @Throws(DuplicateNameException::class)
    override suspend fun insert(name: String, order: Long) {
        if (checkDuplicateName(name)) throw DuplicateNameException(name)
        handler.await {
            categoriesQueries.insert(
                name = name,
                order = order,
                flags = 0L,
            )
        }
    }

    @Throws(DuplicateNameException::class)
    override suspend fun update(payload: CategoryUpdate) {
        if (payload.name != null && checkDuplicateName(payload.name)) throw DuplicateNameException(payload.name)
        handler.await {
            categoriesQueries.update(
                name = payload.name,
                order = payload.order,
                flags = payload.flags,
                categoryId = payload.id,
            )
        }
    }

    override suspend fun delete(categoryId: Long) {
        handler.await {
            categoriesQueries.delete(
                categoryId = categoryId,
            )
        }
    }

    override suspend fun checkDuplicateName(name: String): Boolean {
        return handler
            .awaitList { categoriesQueries.getCategories() }
            .any { it.name == name }
    }
}
