package mihon.feature.upcoming.anime

import tachiyomi.domain.entries.anime.model.Anime
import java.time.LocalDate

sealed interface UpcomingAnimeUIModel {
    data class Header(val date: LocalDate, val animeCount: Int) : UpcomingAnimeUIModel
    data class Item(val anime: Anime) : UpcomingAnimeUIModel
}
