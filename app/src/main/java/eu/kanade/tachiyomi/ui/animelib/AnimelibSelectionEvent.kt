package eu.kanade.tachiyomi.ui.animelib

import eu.kanade.domain.anime.model.Anime

sealed class AnimelibSelectionEvent {

    class Selected(val anime: Anime) : AnimelibSelectionEvent()
    class Unselected(val anime: Anime) : AnimelibSelectionEvent()
    object Cleared : AnimelibSelectionEvent()
}
