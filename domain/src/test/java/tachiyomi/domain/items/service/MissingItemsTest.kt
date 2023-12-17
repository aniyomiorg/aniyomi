package tachiyomi.domain.items.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.service.calculateChapterGap
import tachiyomi.domain.items.chapter.service.missingChaptersCount
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.service.calculateEpisodeGap

@Execution(ExecutionMode.CONCURRENT)
class MissingItemsTest {

    @Test
    fun `missingChaptersCount returns 0 when empty list`() {
        emptyList<Double>().missingChaptersCount() shouldBe 0
    }

    @Test
    fun `missingChaptersCount returns 0 when all unknown item numbers`() {
        listOf(-1.0, -1.0, -1.0).missingChaptersCount() shouldBe 0
    }

    @Test
    fun `missingChaptersCount handles repeated base item numbers`() {
        listOf(1.0, 1.0, 1.1, 1.5, 1.6, 1.99).missingChaptersCount() shouldBe 0
    }

    @Test
    fun `missingChaptersCount returns number of missing items`() {
        listOf(-1.0, 1.0, 2.0, 2.2, 4.0, 6.0, 10.0, 10.0).missingChaptersCount() shouldBe 5
    }

    @Test
    fun `calculateChapterGap returns difference`() {
        calculateChapterGap(chapter(10.0), chapter(9.0)) shouldBe 0f
        calculateChapterGap(chapter(10.0), chapter(8.0)) shouldBe 1f
        calculateChapterGap(chapter(10.0), chapter(8.5)) shouldBe 1f
        calculateChapterGap(chapter(10.0), chapter(1.1)) shouldBe 8f

        calculateChapterGap(10.0, 9.0) shouldBe 0f
        calculateChapterGap(10.0, 8.0) shouldBe 1f
        calculateChapterGap(10.0, 8.5) shouldBe 1f
        calculateChapterGap(10.0, 1.1) shouldBe 8f
    }

    @Test
    fun `calculateChapterGap returns 0 if either are not valid chapter numbers`() {
        calculateChapterGap(chapter(-1.0), chapter(10.0)) shouldBe 0
        calculateChapterGap(chapter(99.0), chapter(-1.0)) shouldBe 0

        calculateChapterGap(-1.0, 10.0) shouldBe 0
        calculateChapterGap(99.0, -1.0) shouldBe 0
    }

    private fun chapter(number: Double) = Chapter.create().copy(
        chapterNumber = number,
    )

    @Test
    fun `calculateEpisodeGap returns difference`() {
        calculateEpisodeGap(episode(10.0), episode(9.0)) shouldBe 0f
        calculateEpisodeGap(episode(10.0), episode(8.0)) shouldBe 1f
        calculateEpisodeGap(episode(10.0), episode(8.5)) shouldBe 1f
        calculateEpisodeGap(episode(10.0), episode(1.1)) shouldBe 8f

        calculateEpisodeGap(10.0, 9.0) shouldBe 0f
        calculateEpisodeGap(10.0, 8.0) shouldBe 1f
        calculateEpisodeGap(10.0, 8.5) shouldBe 1f
        calculateEpisodeGap(10.0, 1.1) shouldBe 8f
    }

    @Test
    fun `calculateEpisodeGap returns 0 if either are not valid episode numbers`() {
        calculateEpisodeGap(episode(-1.0), episode(10.0)) shouldBe 0
        calculateEpisodeGap(episode(99.0), episode(-1.0)) shouldBe 0

        calculateEpisodeGap(-1.0, 10.0) shouldBe 0
        calculateEpisodeGap(99.0, -1.0) shouldBe 0
    }

    private fun episode(number: Double) = Episode.create().copy(
        episodeNumber = number,
    )
}
