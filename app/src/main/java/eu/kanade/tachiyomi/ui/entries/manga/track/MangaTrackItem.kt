package eu.kanade.tachiyomi.ui.entries.manga.track

import eu.kanade.tachiyomi.data.track.TrackService
import tachiyomi.domain.track.manga.model.MangaTrack

data class MangaTrackItem(val track: MangaTrack?, val service: TrackService)
