package tachiyomi.domain.library.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.manga.model.MangaLibrarySort

class LibraryFlagsTest {

    @Test
    fun `Check the amount of flags`() {
        assertEquals(4, LibraryDisplayMode.values.size)
        assertEquals(8, MangaLibrarySort.types.size)
        assertEquals(2, MangaLibrarySort.directions.size)
        assertEquals(8, AnimeLibrarySort.types.size)
        assertEquals(2, AnimeLibrarySort.directions.size)
    }

    @Test
    fun `Test Flag plus operator (LibraryDisplayMode)`() {
        val current = LibraryDisplayMode.List
        val new = LibraryDisplayMode.CoverOnlyGrid
        val flag = current + new

        assertEquals(0b00000011, flag)
    }

    @Test
    fun `Test Flag plus operator (LibrarySort)`() {
        val mangacurrent = MangaLibrarySort(MangaLibrarySort.Type.LastRead, MangaLibrarySort.Direction.Ascending)
        val animecurrent = AnimeLibrarySort(AnimeLibrarySort.Type.LastSeen, AnimeLibrarySort.Direction.Ascending)
        val newmanga = MangaLibrarySort(MangaLibrarySort.Type.DateAdded, MangaLibrarySort.Direction.Ascending)
        val newanime = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val mangaflag = mangacurrent + newmanga
        val animeflag = animecurrent + newanime

        assertEquals(0b01011100, mangaflag)
        assertEquals(0b01011100, animeflag)
    }

    @Test
    fun `Test Flag plus operator`() {
        val display = LibraryDisplayMode.CoverOnlyGrid
        val mangasort = MangaLibrarySort(MangaLibrarySort.Type.DateAdded, MangaLibrarySort.Direction.Ascending)
        val animesort = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val mangaflag = display + mangasort
        val animeflag = display + animesort

        assertEquals(0b01011111, mangaflag)
        assertEquals(0b01011111, animeflag)
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

        assertEquals(0b00001110, currentmangaFlag)
        assertEquals(0b01011111, mangaflag)
        assertNotEquals(currentmangaFlag, mangaflag)
        assertEquals(0b00001110, currentanimeFlag)
        assertEquals(0b01011111, animeflag)
        assertNotEquals(currentanimeFlag, animeflag)
    }

    @Test
    fun `Test default flags`() {
        val mangasort = MangaLibrarySort.default
        val animesort = AnimeLibrarySort.default
        val display = LibraryDisplayMode.default
        val mangaflag = display + mangasort.type + mangasort.direction
        val animeflag = display + animesort.type + animesort.direction

        assertEquals(0b01000000, mangaflag)
        assertEquals(0b01000000, animeflag)
    }
}
