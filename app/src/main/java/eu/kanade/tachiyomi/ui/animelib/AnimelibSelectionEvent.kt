package eu.kanade.tachiyomi.ui.animelib

import eu.kanade.tachiyomi.data.database.models.Anime

sealed class AnimelibSelectionEvent {

    class Selected(val anime: Anime) : AnimelibSelectionEvent()
    class Unselected(val anime: Anime) : AnimelibSelectionEvent()
    class Cleared : AnimelibSelectionEvent()
}
