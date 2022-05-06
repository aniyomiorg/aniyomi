package eu.kanade.data.animehistory

import eu.kanade.domain.animehistory.model.AnimeHistory
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import java.util.Date

val animehistoryMapper: (Long, Long, Date?, Date?) -> AnimeHistory = { id, episodeId, seenAt, _ ->
    AnimeHistory(
        id = id,
        episodeId = episodeId,
        seenAt = seenAt,
    )
}

val animehistoryWithRelationsMapper: (Long, Long, Long, String, String?, Float, Date?) -> AnimeHistoryWithRelations = {
    historyId, animeId, episodeId, title, thumbnailUrl, episodeNumber, seenAt ->
    AnimeHistoryWithRelations(
        id = historyId,
        episodeId = episodeId,
        animeId = animeId,
        title = title,
        thumbnailUrl = thumbnailUrl ?: "",
        episodeNumber = episodeNumber,
        seenAt = seenAt
    )
}
