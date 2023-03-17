package eu.kanade.tachiyomi.ui.entries.manga.track

import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.TrackService

data class MangaTrackItem(val track: MangaTrack?, val service: TrackService)
