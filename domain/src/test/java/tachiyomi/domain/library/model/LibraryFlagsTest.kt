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
        MangaLibrarySort.types.size shouldBe 8
        MangaLibrarySort.directions.size shouldBe 2
        AnimeLibrarySort.types.size shouldBe 9
        AnimeLibrarySort.directions.size shouldBe 2
    }

    @Test
    fun `Test Flag plus operator (LibraryDisplayMode)`() {
        val current = LibraryDisplayMode.List
        val new = LibraryDisplayMode.CoverOnlyGrid
        val flag = current + new

        flag shouldBe 0b00000011
    }

    @Test
    fun `Test Flag plus operator (LibrarySort)`() {
        val mangacurrent = MangaLibrarySort(MangaLibrarySort.Type.LastRead, MangaLibrarySort.Direction.Ascending)
        val animecurrent = AnimeLibrarySort(AnimeLibrarySort.Type.LastSeen, AnimeLibrarySort.Direction.Ascending)
        val newmanga = MangaLibrarySort(MangaLibrarySort.Type.DateAdded, MangaLibrarySort.Direction.Ascending)
        val newanime = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val mangaflag = mangacurrent + newmanga
        val animeflag = animecurrent + newanime

        mangaflag shouldBe 0b01011100
        animeflag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator`() {
        val display = LibraryDisplayMode.CoverOnlyGrid
        val mangasort = MangaLibrarySort(MangaLibrarySort.Type.DateAdded, MangaLibrarySort.Direction.Ascending)
        val animesort = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val mangaflag = display + mangasort
        val animeflag = display + animesort

        mangaflag shouldBe 0b01011111
        animeflag shouldBe 0b01011111
    }

    @Test
    fun `Test Flag plus operator with old flag as base`() {
        val currentDisplay = LibraryDisplayMode.List
        val currentmangaSort = MangaLibrarySort(MangaLibrarySort.Type.UnreadCount, MangaLibrarySort.Direction.Descending)
        val currentmangaFlag = currentDisplay + currentmangaSort
        val currentanimeSort = AnimeLibrarySort(AnimeLibrarySort.Type.UnseenCount, AnimeLibrarySort.Direction.Descending)
        val currentanimeFlag = currentDisplay + currentanimeSort

        val display = LibraryDisplayMode.CoverOnlyGrid
        val mangasort = MangaLibrarySort(MangaLibrarySort.Type.DateAdded, MangaLibrarySort.Direction.Ascending)
        val mangaflag = currentmangaFlag + display + mangasort
        val animesort = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val animeflag = currentanimeFlag + display + animesort

        currentmangaFlag shouldBe 0b00001110
        mangaflag shouldBe 0b01011111
        mangaflag shouldNotBe currentmangaFlag
        currentanimeFlag shouldBe 0b00001110
        animeflag shouldBe 0b01011111
        animeflag shouldNotBe currentanimeFlag
    }

    @Test
    fun `Test default flags`() {
        val mangasort = MangaLibrarySort.default
        val animesort = AnimeLibrarySort.default
        val display = LibraryDisplayMode.default
        val mangaflag = display + mangasort.type + mangasort.direction
        val animeflag = display + animesort.type + animesort.direction

        mangaflag shouldBe 0b01000000
        animeflag shouldBe 0b01000000
    }
}
