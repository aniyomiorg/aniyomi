package eu.kanade.data

import androidx.paging.PagingSource
import com.squareup.sqldelight.Query
import eu.kanade.tachiyomi.mi.AnimeDatabase
import kotlinx.coroutines.flow.Flow

interface AnimeDatabaseHandler {

    suspend fun <T> await(inTransaction: Boolean = false, block: suspend AnimeDatabase.() -> T): T

    suspend fun <T : Any> awaitList(
        inTransaction: Boolean = false,
        block: suspend AnimeDatabase.() -> Query<T>,
    ): List<T>

    suspend fun <T : Any> awaitOne(
        inTransaction: Boolean = false,
        block: suspend AnimeDatabase.() -> Query<T>,
    ): T

    suspend fun <T : Any> awaitOneOrNull(
        inTransaction: Boolean = false,
        block: suspend AnimeDatabase.() -> Query<T>,
    ): T?

    fun <T : Any> subscribeToList(block: AnimeDatabase.() -> Query<T>): Flow<List<T>>

    fun <T : Any> subscribeToOne(block: AnimeDatabase.() -> Query<T>): Flow<T>

    fun <T : Any> subscribeToOneOrNull(block: AnimeDatabase.() -> Query<T>): Flow<T?>

    fun <T : Any> subscribeToPagingSource(
        countQuery: AnimeDatabase.() -> Query<Long>,
        queryProvider: AnimeDatabase.(Long, Long) -> Query<T>,
    ): PagingSource<Long, T>
}
