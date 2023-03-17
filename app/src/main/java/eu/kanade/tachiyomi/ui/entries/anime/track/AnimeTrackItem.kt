package eu.kanade.tachiyomi.ui.entries.anime.track

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackService

data class AnimeTrackItem(val track: AnimeTrack?, val service: TrackService)
