package tachiyomi.domain.entries.anime.interactor

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.items.episode.model.Episode
import java.time.Duration
import java.time.ZonedDateTime

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
        val episodes = mutableListOf<Episode>()
        (1..1).forEach {
            val duration = Duration.ofHours(10)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    @Test
    fun `calculateInterval returns 7 when 5 episodes in 1 day`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(10)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    @Test
    fun `calculateInterval returns 7 when 7 episodes in 48 hours, 2 day`() {
        val episodes = mutableListOf<Episode>()
        (1..2).forEach {
            val duration = Duration.ofHours(24L)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        (1..5).forEach {
            val duration = Duration.ofHours(48L)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    @Test
    fun `calculateInterval returns default of 1 day when interval less than 1`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(15L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    // Normal interval calculation
    @Test
    fun `calculateInterval returns 1 when 5 episodes in 120 hours, 5 days`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(24L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns 2 when 5 episodes in 240 hours, 10 days`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(48L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 2
    }

    @Test
    fun `calculateInterval returns floored value when interval is decimal`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(25L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns 1 when 5 episodes in 215 hours, 5 days`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(43L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns interval based on fetch time if upload time not available`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(25L * it)
            val newEpisode = episodeAddTime(episode, duration).copy(dateUpload = 0L)
            episodes.add(newEpisode)
        }
        setAnimeFetchInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    private fun episodeAddTime(episode: Episode, duration: Duration): Episode {
        val newTime = testTime.plus(duration).toEpochSecond() * 1000
        return episode.copy(dateFetch = newTime, dateUpload = newTime)
    }
}
