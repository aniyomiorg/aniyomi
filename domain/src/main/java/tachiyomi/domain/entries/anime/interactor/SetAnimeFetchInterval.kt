package tachiyomi.domain.entries.anime.interactor

import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.items.episode.interactor.GetEpisodeByAnimeId
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.api.get

const val MAX_FETCH_INTERVAL = 28
private const val FETCH_INTERVAL_GRACE_PERIOD = 1

class SetAnimeFetchInterval(
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId,
) {

    suspend fun toAnimeUpdateOrNull(
        anime: Anime,
        dateTime: ZonedDateTime,
        window: Pair<Long, Long>,
    ): AnimeUpdate? {
        val currentWindow = if (window.first == 0L && window.second == 0L) {
            getWindow(ZonedDateTime.now())
        } else {
            window
        }
        val episodes = getEpisodeByAnimeId.await(anime.id)
        val interval = anime.fetchInterval.takeIf { it < 0 } ?: calculateInterval(
            episodes,
            dateTime
        )
        val nextUpdate = calculateNextUpdate(anime, interval, dateTime, currentWindow)

        return if (anime.nextUpdate == nextUpdate && anime.fetchInterval == interval) {
            null
        } else {
            AnimeUpdate(id = anime.id, nextUpdate = nextUpdate, fetchInterval = interval)
        }
    }

    fun getWindow(dateTime: ZonedDateTime): Pair<Long, Long> {
        val today = dateTime.toLocalDate().atStartOfDay(dateTime.zone)
        val lowerBound = today.minusDays(FETCH_INTERVAL_GRACE_PERIOD.toLong())
        val upperBound = today.plusDays(FETCH_INTERVAL_GRACE_PERIOD.toLong())
        return Pair(lowerBound.toEpochSecond() * 1000, upperBound.toEpochSecond() * 1000 - 1)
    }

    internal fun calculateInterval(episodes: List<Episode>, zonedDateTime: ZonedDateTime): Int {
        val sortedEpisodes = episodes
            .sortedWith(
                compareByDescending<Episode> { it.dateUpload }.thenByDescending { it.dateFetch }
            )
            .take(50)

        val uploadDates = sortedEpisodes
            .filter { it.dateUpload > 0L }
            .map {
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.dateUpload), zonedDateTime.zone)
                    .toLocalDate()
                    .atStartOfDay()
            }
            .distinct()
        val fetchDates = sortedEpisodes
            .map {
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.dateFetch), zonedDateTime.zone)
                    .toLocalDate()
                    .atStartOfDay()
            }
            .distinct()

        val interval = when {
            // Enough upload date from source
            uploadDates.size >= 3 -> {
                val uploadDelta = uploadDates.last().until(uploadDates.first(), ChronoUnit.DAYS)
                val uploadPeriod = uploadDates.indexOf(uploadDates.last())
                uploadDelta.floorDiv(uploadPeriod).toInt()
            }
            // Enough fetch date from client
            fetchDates.size >= 3 -> {
                val fetchDelta = fetchDates.last().until(fetchDates.first(), ChronoUnit.DAYS)
                val uploadPeriod = fetchDates.indexOf(fetchDates.last())
                fetchDelta.floorDiv(uploadPeriod).toInt()
            }
            // Default to 7 days
            else -> 7
        }

        return interval.coerceIn(1, MAX_FETCH_INTERVAL)
    }

    private fun calculateNextUpdate(
        anime: Anime,
        interval: Int,
        dateTime: ZonedDateTime,
        window: Pair<Long, Long>,
    ): Long {
        return if (
            anime.nextUpdate !in window.first.rangeTo(window.second + 1) ||
            anime.fetchInterval == 0
        ) {
            val latestDate = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(anime.lastUpdate),
                dateTime.zone
            )
                .toLocalDate()
                .atStartOfDay()
            val timeSinceLatest = ChronoUnit.DAYS.between(latestDate, dateTime).toInt()
            val cycle = timeSinceLatest.floorDiv(
                interval.absoluteValue.takeIf { interval < 0 }
                    ?: doubleInterval(interval, timeSinceLatest, doubleWhenOver = 10),
            )
            latestDate.plusDays((cycle + 1) * interval.toLong()).toEpochSecond(dateTime.offset) * 1000
        } else {
            anime.nextUpdate
        }
    }

    private fun doubleInterval(delta: Int, timeSinceLatest: Int, doubleWhenOver: Int): Int {
        if (delta >= MAX_FETCH_INTERVAL) return MAX_FETCH_INTERVAL

        // double delta again if missed more than 9 check in new delta
        val cycle = timeSinceLatest.floorDiv(delta) + 1
        return if (cycle > doubleWhenOver) {
            doubleInterval(delta * 2, timeSinceLatest, doubleWhenOver)
        } else {
            delta
        }
    }
}
