package mihon.feature.upcoming.manga

import tachiyomi.domain.entries.manga.model.Manga
import java.time.LocalDate

sealed interface UpcomingMangaUIModel {
    data class Header(val date: LocalDate) : UpcomingMangaUIModel
    data class Item(val manga: Manga) : UpcomingMangaUIModel
}
