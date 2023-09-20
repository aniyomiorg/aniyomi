package eu.kanade.tachiyomi.ui.entries.anime.track

import eu.kanade.tachiyomi.data.track.TrackService
import tachiyomi.domain.track.anime.model.AnimeTrack

data class AnimeTrackItem(val track: AnimeTrack?, val service: TrackService)
