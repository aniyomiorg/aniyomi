package tachiyomi.domain.entries.anime.interactor

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.items.episode.model.Episode

@Execution(ExecutionMode.CONCURRENT)
class SetAnimeFetchIntervalTest {

    private val testTime = ZonedDateTime.parse("2020-01-01T00:00:00Z")
    private var episode = Episode.create().copy(
        dateFetch = testTime.toEpochSecond() * 1000,
        dateUpload = testTime.toEpochSecond() * 1000,
    )

    private val setAnimeFetchInterval = SetAnimeFetchInterval(mockk())

    @Test
    fun `calculateInterval returns default of 7 days when less than 3 distinct days`() {
        val episodes = (1..2).map {
            episodeWithTime(episode, 10.hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    @Test
    fun `calculateInterval returns 7 when 5 episodes in 1 day`() {
        val episodes = (1..5).map {
            episodeWithTime(episode, 10.hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    @Test
    fun `calculateInterval returns 7 when 7 episodes in 48 hours, 2 day`() {
        val episodes = (1..2).map {
            episodeWithTime(episode, 24.hours)
        } + (1..5).map {
            episodeWithTime(episode, 48.hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    @Test
    fun `calculateInterval returns default of 1 day when interval less than 1`() {
        val episodes = (1..5).map {
            episodeWithTime(episode, (15 * it).hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    // Normal interval calculation
    @Test
    fun `calculateInterval returns 1 when 5 episodes in 120 hours, 5 days`() {
        val episodes = (1..5).map {
            episodeWithTime(episode, (24 * it).hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns 2 when 5 episodes in 240 hours, 10 days`() {
        val episodes = (1..5).map {
            episodeWithTime(episode, (48 * it).hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 2
    }

    @Test
    fun `calculateInterval returns floored value when interval is decimal`() {
        val episodes = (1..5).map {
            episodeWithTime(episode, (25 * it).hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns 1 when 5 episodes in 215 hours, 5 days`() {
        val episodes = (1..5).map {
            episodeWithTime(episode, (43 * it).hours)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns interval based on fetch time if upload time not available`() {
        val episodes = (1..5).map {
            episodeWithTime(episode, (25 * it).hours).copy(dateUpload = 0L)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    private fun episodeWithTime(episode: Episode, duration: Duration): Episode {
        val newTime = testTime.plus(duration.toJavaDuration()).toEpochSecond() * 1000
        return episode.copy(dateFetch = newTime, dateUpload = newTime)
    }
}
