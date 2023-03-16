package eu.kanade.tachiyomi.ui.manga.track

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService

data class MangaTrackItem(val track: Track?, val service: TrackService)
