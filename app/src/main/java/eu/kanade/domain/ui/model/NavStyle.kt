package eu.kanade.domain.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import tachiyomi.i18n.MR

enum class NavStyle(
    val titleRes: StringResource,
    val tabs: List<Tab>,
    val moreTab: Tab,
) {
    MOVE_MANGA_TO_MORE(
        titleRes = MR.strings.pref_bottom_nav_no_manga,
        tabs = listOf(
            AnimeLibraryTab,
            UpdatesTab(fromMore = false, inMiddle = false),
            HistoriesTab(fromMore = false),
            BrowseTab(),
            MoreTab,
        ),
        moreTab = MangaLibraryTab,
    ),
    MOVE_UPDATES_TO_MORE(
        titleRes = MR.strings.pref_bottom_nav_no_updates,
        tabs = listOf(
            AnimeLibraryTab,
            MangaLibraryTab,
            HistoriesTab(fromMore = false),
            BrowseTab(),
            MoreTab,
        ),
        moreTab = UpdatesTab(fromMore = true, inMiddle = false),
    ),
    MOVE_HISTORY_TO_MORE(
        titleRes = MR.strings.pref_bottom_nav_no_history,
        tabs = listOf(
            AnimeLibraryTab,
            MangaLibraryTab,
            UpdatesTab(fromMore = false, inMiddle = true),
            BrowseTab(),
            MoreTab,
        ),
        moreTab = HistoriesTab(fromMore = true),
    );

    val moreIcon: ImageVector
        @Composable
        get() = when(this) {
            MOVE_MANGA_TO_MORE -> Icons.Outlined.CollectionsBookmark
            MOVE_UPDATES_TO_MORE -> ImageVector.vectorResource(id = R.drawable.ic_updates_outline_24dp)
            MOVE_HISTORY_TO_MORE -> Icons.Outlined.History
        }
}
