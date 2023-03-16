package eu.kanade.tachiyomi.ui.anime.track

import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackService

data class AnimeTrackItem(val track: AnimeTrack?, val service: TrackService)
