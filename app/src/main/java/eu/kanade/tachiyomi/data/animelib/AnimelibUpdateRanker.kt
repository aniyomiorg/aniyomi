package eu.kanade.tachiyomi.data.animelib

import eu.kanade.tachiyomi.data.database.models.Anime
import java.util.Collections
import kotlin.Comparator
import kotlin.math.abs

/**
 * This class will provide various functions to rank manga to efficiently schedule manga to update.
 */
object AnimelibUpdateRanker {

    val rankingScheme = listOf(
        (this::lexicographicRanking)(),
        (this::latestFirstRanking)(),
        (this::nextFirstRanking)()
    )

    /**
     * Provides a total ordering over all the Mangas.
     *
     * Orders the manga based on the distance between the next expected update and now.
     * The comparator is reversed, placing the smallest (and thus closest to updating now) first.
     */
    fun nextFirstRanking(): Comparator<Anime> {
        val time = System.currentTimeMillis()
        return Collections.reverseOrder(
            Comparator { animeFirst: Anime,
                animeSecond: Anime ->
                compareValues(abs(animeSecond.next_update - time), abs(animeFirst.next_update - time))
            }
        )
    }

    /**
     * Provides a total ordering over all the [Manga]s.
     *
     * Assumption: An active [Manga] mActive is expected to have been last updated after an
     * inactive [Manga] mInactive.
     *
     * Using this insight, function returns a Comparator for which mActive appears before mInactive.
     * @return a Comparator that ranks manga based on relevance.
     */
    private fun latestFirstRanking(): Comparator<Anime> =
        Comparator { first: Anime, second: Anime ->
            compareValues(second.last_update, first.last_update)
        }

    /**
     * Provides a total ordering over all the [Anime]s.
     *
     * Order the manga lexicographically.
     * @return a Comparator that ranks manga lexicographically based on the title.
     */
    private fun lexicographicRanking(): Comparator<Anime> =
        Comparator { first: Anime, second: Anime ->
            compareValues(first.title, second.title)
        }
}
