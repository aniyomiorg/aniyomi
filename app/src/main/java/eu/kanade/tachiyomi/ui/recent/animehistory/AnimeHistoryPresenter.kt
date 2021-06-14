package eu.kanade.tachiyomi.ui.recent.animehistory

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeEpisodeHistory
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.DateSectionItem
import eu.kanade.tachiyomi.util.lang.toDateKey
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.injectLazy
import java.util.Calendar
import java.util.Date
import java.util.TreeMap

/**
 * Presenter of AnimeHistoryFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class AnimeHistoryPresenter : BasePresenter<AnimeHistoryController>() {

    /**
     * Used to connect to database
     */
    val db: AnimeDatabaseHelper by injectLazy()

    private var recentAnimeSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Used to get a list of recently read anime
        updateList()
    }

    fun requestNext(offset: Int, search: String = "") {
        getRecentAnimeObservable(offset = offset, search = search)
            .subscribeLatestCache(
                { view, animes ->
                    view.onNextAnime(animes)
                },
                AnimeHistoryController::onAddPageError
            )
    }

    /**
     * Get recent anime observable
     * @return list of animehistory
     */
    private fun getRecentAnimeObservable(limit: Int = 25, offset: Int = 0, search: String = ""): Observable<List<AnimeHistoryItem>> {
        // Set date limit for recent anime
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.YEAR, -50)
        }

        return db.getRecentAnime(cal.time, limit, offset, search).asRxObservable()
            .map { recents ->
                val map = TreeMap<Date, MutableList<AnimeEpisodeHistory>> { d1, d2 -> d2.compareTo(d1) }
                val byDay = recents
                    .groupByTo(map, { it.animehistory.last_seen.toDateKey() })
                byDay.flatMap { entry ->
                    val dateItem = DateSectionItem(entry.key)
                    entry.value.map { AnimeHistoryItem(it, dateItem) }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Reset last read of chapter to 0L
     * @param animehistory animehistory belonging to chapter
     */
    fun removeFromAnimeHistory(animehistory: AnimeHistory) {
        animehistory.last_seen = 0L
        db.updateAnimeHistoryLastSeen(animehistory).asRxObservable()
            .subscribe()
    }

    /**
     * Pull a list of animehistory from the db
     * @param search a search query to use for filtering
     */
    fun updateList(search: String = "") {
        recentAnimeSubscription?.unsubscribe()
        recentAnimeSubscription = getRecentAnimeObservable(search = search)
            .subscribeLatestCache(
                { view, animes ->
                    view.onNextAnime(animes, true)
                },
                AnimeHistoryController::onAddPageError
            )
    }

    /**
     * Removes all chapters belonging to anime from animehistory.
     * @param animeId id of anime
     */
    fun removeAllFromAnimeHistory(animeId: Long) {
        db.getHistoryByAnimeId(animeId).asRxSingle()
            .map { list ->
                list.forEach { it.last_seen = 0L }
                db.updateAnimeHistoryLastSeen(list).executeAsBlocking()
            }
            .subscribe()
    }

    /**
     * Retrieves the next chapter of the given one.
     *
     * @param episode the chapter of the animehistory object.
     * @param anime the anime of the chapter.
     */
    fun getNextEpisode(episode: Episode, anime: Anime): Episode? {
        if (!episode.seen) {
            return episode
        }

        val sortFunction: (Episode, Episode) -> Int = when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Anime.EPISODE_SORTING_NUMBER -> { c1, c2 -> c1.episode_number.compareTo(c2.episode_number) }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val chapters = db.getEpisodes(anime).executeAsBlocking()
            .sortedWith { c1, c2 -> sortFunction(c1, c2) }

        val currEpisodeIndex = chapters.indexOfFirst { episode.id == it.id }
        return when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> chapters.getOrNull(currEpisodeIndex + 1)
            Anime.EPISODE_SORTING_NUMBER -> {
                val chapterNumber = episode.episode_number

                ((currEpisodeIndex + 1) until chapters.size)
                    .map { chapters[it] }
                    .firstOrNull {
                        it.episode_number > chapterNumber &&
                            it.episode_number <= chapterNumber + 1
                    }
            }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> {
                chapters.drop(currEpisodeIndex + 1)
                    .firstOrNull { it.date_upload >= episode.date_upload }
            }
            else -> throw NotImplementedError("Unknown sorting method")
        }
    }
}
