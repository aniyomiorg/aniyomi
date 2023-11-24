package eu.kanade.tachiyomi.ui.entries.manga.track

import eu.kanade.tachiyomi.data.track.Tracker
import tachiyomi.domain.track.manga.model.MangaTrack

data class MangaTrackItem(val track: MangaTrack?, val tracker: Tracker)
