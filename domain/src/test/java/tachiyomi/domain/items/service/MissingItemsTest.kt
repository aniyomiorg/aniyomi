package tachiyomi.domain.items.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.episode.model.Episode

@Execution(ExecutionMode.CONCURRENT)
class MissingItemsTest {

    @Test
    fun `missingItemsCount returns 0 when empty list`() {
        emptyList<Float>().missingItemsCount() shouldBe 0
    }

    @Test
    fun `missingItemsCount returns 0 when all unknown item numbers`() {
        listOf(-1f, -1f, -1f).missingItemsCount() shouldBe 0
    }

    @Test
    fun `missingItemsCount handles repeated base item numbers`() {
        listOf(1f, 1.0f, 1.1f, 1.5f, 1.6f, 1.99f).missingItemsCount() shouldBe 0
    }

    @Test
    fun `missingItemsCount returns number of missing items`() {
        listOf(-1f, 1f, 2f, 2.2f, 4f, 6f, 10f, 11f).missingItemsCount() shouldBe 5
    }

    @Test
    fun `calculateChapterGap returns difference`() {
        calculateChapterGap(chapter(10f), chapter(9f)) shouldBe 0f
        calculateChapterGap(chapter(10f), chapter(8f)) shouldBe 1f
        calculateChapterGap(chapter(10f), chapter(8.5f)) shouldBe 1f
        calculateChapterGap(chapter(10f), chapter(1.1f)) shouldBe 8f

        calculateChapterGap(10f, 9f) shouldBe 0f
        calculateChapterGap(10f, 8f) shouldBe 1f
        calculateChapterGap(10f, 8.5f) shouldBe 1f
        calculateChapterGap(10f, 1.1f) shouldBe 8f
    }

    @Test
    fun `calculateChapterGap returns 0 if either are not valid chapter numbers`() {
        calculateChapterGap(chapter(-1f), chapter(10f)) shouldBe 0
        calculateChapterGap(chapter(99f), chapter(-1f)) shouldBe 0

        calculateChapterGap(-1f, 10f) shouldBe 0
        calculateChapterGap(99f, -1f) shouldBe 0
    }

    private fun chapter(number: Float) = Chapter.create().copy(
        chapterNumber = number,
    )

    @Test
    fun `calculateEpisodeGap returns difference`() {
        calculateEpisodeGap(episode(10f), episode(9f)) shouldBe 0f
        calculateEpisodeGap(episode(10f), episode(8f)) shouldBe 1f
        calculateEpisodeGap(episode(10f), episode(8.5f)) shouldBe 1f
        calculateEpisodeGap(episode(10f), episode(1.1f)) shouldBe 8f

        calculateEpisodeGap(10f, 9f) shouldBe 0f
        calculateEpisodeGap(10f, 8f) shouldBe 1f
        calculateEpisodeGap(10f, 8.5f) shouldBe 1f
        calculateEpisodeGap(10f, 1.1f) shouldBe 8f
    }

    @Test
    fun `calculateEpisodeGap returns 0 if either are not valid episode numbers`() {
        calculateEpisodeGap(episode(-1f), episode(10f)) shouldBe 0
        calculateEpisodeGap(episode(99f), episode(-1f)) shouldBe 0

        calculateEpisodeGap(-1f, 10f) shouldBe 0
        calculateEpisodeGap(99f, -1f) shouldBe 0
    }

    private fun episode(number: Float) = Episode.create().copy(
        episodeNumber = number,
    )
}
