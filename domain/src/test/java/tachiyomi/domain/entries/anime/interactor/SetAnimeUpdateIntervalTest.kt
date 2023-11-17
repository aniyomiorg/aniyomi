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
class SetAnimeUpdateIntervalTest {
    private val testTime = ZonedDateTime.parse("2020-01-01T00:00:00Z")
    private var episode = Episode.create().copy(
        dateFetch = testTime.toEpochSecond() * 1000,
        dateUpload = testTime.toEpochSecond() * 1000,
    )

    private val setAnimeUpdateInterval = SetAnimeUpdateInterval(mockk())

    private fun episodeAddTime(episode: Episode, duration: Duration): Episode {
        val newTime = testTime.plus(duration).toEpochSecond() * 1000
        return episode.copy(dateFetch = newTime, dateUpload = newTime)
    }

    // default 7 when less than 3 distinct day
    @Test
    fun `calculateInterval returns 7 when 1 episodes in 1 day`() {
        val episodes = mutableListOf<Episode>()
        (1..1).forEach {
            val duration = Duration.ofHours(10)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    @Test
    fun `calculateInterval returns 7 when 5 episodes in 1 day`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(10)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 7
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
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 7
    }

    // Default 1 if interval less than 1
    @Test
    fun `calculateInterval returns 1 when 5 episodes in 75 hours, 3 days`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(15L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 1
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
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns 2 when 5 episodes in 240 hours, 10 days`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(48L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 2
    }

    // If interval is decimal, floor to closest integer
    @Test
    fun `calculateInterval returns 1 when 5 episodes in 125 hours, 5 days`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(25L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    @Test
    fun `calculateInterval returns 1 when 5 episodes in 215 hours, 5 days`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(43L * it)
            val newEpisode = episodeAddTime(episode, duration)
            episodes.add(newEpisode)
        }
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 1
    }

    // Use fetch time if upload time not available
    @Test
    fun `calculateInterval returns 1 when 5 episodes in 125 hours, 5 days of dateFetch`() {
        val episodes = mutableListOf<Episode>()
        (1..5).forEach {
            val duration = Duration.ofHours(25L * it)
            val newEpisode = episodeAddTime(episode, duration).copy(dateUpload = 0L)
            episodes.add(newEpisode)
        }
        setAnimeUpdateInterval.calculateInterval(episodes, testTime) shouldBe 1
    }
}
