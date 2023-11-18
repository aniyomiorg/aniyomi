package tachiyomi.domain.entries.anime.interactor

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

const val MAX_GRACE_PERIOD = 28

class SetAnimeFetchInterval(
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) {

    fun update(
        anime: Anime,
        episodes: List<Episode>,
        zonedDateTime: ZonedDateTime,
        fetchRange: Pair<Long, Long>,
    ): AnimeUpdate? {
        val currentInterval = if (fetchRange.first == 0L && fetchRange.second == 0L) {
            getCurrent(ZonedDateTime.now())
        } else {
            fetchRange
        }
        val interval = anime.fetchInterval.takeIf { it < 0 } ?: calculateInterval(episodes, zonedDateTime)
        val nextUpdate = calculateNextUpdate(anime, interval, zonedDateTime, currentInterval)

        return if (anime.nextUpdate == nextUpdate && anime.fetchInterval == interval) {
            null
        } else {
            AnimeUpdate(id = anime.id, nextUpdate = nextUpdate, fetchInterval = interval)
        }
    }

    fun getCurrent(timeToCal: ZonedDateTime): Pair<Long, Long> {
        // lead range and the following range depend on if updateOnlyExpectedPeriod set.
        var followRange = 0
        var leadRange = 0
        if (LibraryPreferences.ENTRY_OUTSIDE_RELEASE_PERIOD in libraryPreferences.libraryUpdateItemRestriction().get()) {
            followRange = libraryPreferences.followingAnimeExpectedDays().get()
            leadRange = libraryPreferences.leadingAnimeExpectedDays().get()
        }
        val startToday = timeToCal.toLocalDate().atStartOfDay(timeToCal.zone)
        // revert math of (next_update + follow < now) become (next_update < now - follow)
        // so (now - follow) become lower limit
        val lowerRange = startToday.minusDays(followRange.toLong())
        val higherRange = startToday.plusDays(leadRange.toLong())
        return Pair(lowerRange.toEpochSecond() * 1000, higherRange.toEpochSecond() * 1000 - 1)
    }

    internal fun calculateInterval(episodes: List<Episode>, zonedDateTime: ZonedDateTime): Int {
        val sortedEpisodes = episodes
            .sortedWith(compareByDescending<Episode> { it.dateUpload }.thenByDescending { it.dateFetch })
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
        // Min 1, max 28 days
        return interval.coerceIn(1, MAX_GRACE_PERIOD)
    }

    private fun calculateNextUpdate(
        anime: Anime,
        interval: Int,
        zonedDateTime: ZonedDateTime,
        fetchRange: Pair<Long, Long>,
    ): Long {
        return if (
            anime.nextUpdate !in fetchRange.first.rangeTo(fetchRange.second + 1) ||
            anime.fetchInterval == 0
        ) {
            val latestDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(anime.lastUpdate), zonedDateTime.zone).toLocalDate().atStartOfDay()
            val timeSinceLatest = ChronoUnit.DAYS.between(latestDate, zonedDateTime).toInt()
            val cycle = timeSinceLatest.floorDiv(interval.absoluteValue.takeIf { interval < 0 } ?: doubleInterval(interval, timeSinceLatest, doubleWhenOver = 10, maxValue = 28))
            latestDate.plusDays((cycle + 1) * interval.toLong()).toEpochSecond(zonedDateTime.offset) * 1000
        } else {
            anime.nextUpdate
        }
    }

    private fun doubleInterval(delta: Int, timeSinceLatest: Int, doubleWhenOver: Int, maxValue: Int): Int {
        if (delta >= maxValue) return maxValue
        val cycle = timeSinceLatest.floorDiv(delta) + 1
        // double delta again if missed more than 9 check in new delta
        return if (cycle > doubleWhenOver) {
            doubleInterval(delta * 2, timeSinceLatest, doubleWhenOver, maxValue)
        } else {
            delta
        }
    }
}
