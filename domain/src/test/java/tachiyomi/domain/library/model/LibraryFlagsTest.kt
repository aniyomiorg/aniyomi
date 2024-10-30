package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.manga.model.MangaLibrarySort

@Execution(ExecutionMode.CONCURRENT)
class LibraryFlagsTest {

    @Test
    fun `Check the amount of flags`() {
        LibraryDisplayMode.values.size shouldBe 4
        MangaLibrarySort.types.size shouldBe 10
        MangaLibrarySort.directions.size shouldBe 2
        AnimeLibrarySort.types.size shouldBe 11
        AnimeLibrarySort.directions.size shouldBe 2
    }

    @Test
    fun `Test Flag plus operator (LibrarySort)`() {
        val mangacurrent = MangaLibrarySort(
            MangaLibrarySort.Type.LastRead,
            MangaLibrarySort.Direction.Ascending,
        )
        val animecurrent = AnimeLibrarySort(
            AnimeLibrarySort.Type.LastSeen,
            AnimeLibrarySort.Direction.Ascending,
        )
        val newmanga = MangaLibrarySort(
            MangaLibrarySort.Type.DateAdded,
            MangaLibrarySort.Direction.Ascending,
        )
        val newanime = AnimeLibrarySort(
            AnimeLibrarySort.Type.DateAdded,
            AnimeLibrarySort.Direction.Ascending,
        )
        val mangaflag = mangacurrent + newmanga
        val animeflag = animecurrent + newanime

        mangaflag shouldBe 0b01011100
        animeflag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator`() {
        val mangasort = MangaLibrarySort(
            MangaLibrarySort.Type.DateAdded,
            MangaLibrarySort.Direction.Ascending,
        )
        val animesort = AnimeLibrarySort(
            AnimeLibrarySort.Type.DateAdded,
            AnimeLibrarySort.Direction.Ascending,
        )

        mangasort.flag shouldBe 0b01011100
        animesort.flag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator with old flag as base`() {
        val currentmangaSort = MangaLibrarySort(
            MangaLibrarySort.Type.UnreadCount,
            MangaLibrarySort.Direction.Descending,
        )
        currentmangaSort.flag shouldBe 0b00001100
        val currentanimeSort = AnimeLibrarySort(
            AnimeLibrarySort.Type.UnseenCount,
            AnimeLibrarySort.Direction.Descending,
        )
        currentanimeSort.flag shouldBe 0b00001100

        val mangasort = MangaLibrarySort(
            MangaLibrarySort.Type.DateAdded,
            MangaLibrarySort.Direction.Ascending,
        )
        val mangaflag = currentmangaSort.flag + mangasort
        val animesort = AnimeLibrarySort(
            AnimeLibrarySort.Type.DateAdded,
            AnimeLibrarySort.Direction.Ascending,
        )
        val animeflag = animesort.flag + animesort

        mangaflag shouldBe 0b01011100
        mangaflag shouldNotBe currentmangaSort.flag
        animeflag shouldBe 0b01011100
        animeflag shouldNotBe currentanimeSort.flag
    }

    @Test
    fun `Test default flags`() {
        val mangasort = MangaLibrarySort.default
        val animesort = AnimeLibrarySort.default
        val mangaflag = mangasort.type + mangasort.direction
        val animeflag = animesort.type + animesort.direction

        mangaflag shouldBe 0b01000000
        animeflag shouldBe 0b01000000
    }
}
