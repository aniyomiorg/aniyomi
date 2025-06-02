package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR

enum class StartScreen(val titleRes: StringResource, val tab: Tab) {
    ANIME(AYMR.strings.label_anime, AnimeLibraryTab),
    MANGA(AYMR.strings.manga, MangaLibraryTab),
    UPDATES(MR.strings.label_recent_updates, UpdatesTab),
    HISTORY(MR.strings.label_recent_manga, HistoriesTab),
    BROWSE(MR.strings.browse, BrowseTab),
}
