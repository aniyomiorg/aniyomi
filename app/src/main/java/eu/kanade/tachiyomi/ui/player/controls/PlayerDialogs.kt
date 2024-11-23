package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.data.database.models.anime.Episode
import eu.kanade.tachiyomi.ui.player.Dialogs
import eu.kanade.tachiyomi.ui.player.controls.components.dialogs.EpisodeListDialog
import java.time.format.DateTimeFormatter

@Composable
fun PlayerDialogs(
    dialogShown: Dialogs,

    // Episode list
    episodeDisplayMode: Long,
    currentEpisodeIndex: Int,
    episodeList: List<Episode>,
    dateRelativeTime: Boolean,
    dateFormat: DateTimeFormatter,
    onBookmarkClicked: (Long?, Boolean) -> Unit,
    onEpisodeClicked: (Long?) -> Unit,

    onDismissRequest: () -> Unit,
) {
    when (dialogShown) {
        Dialogs.None -> {}
        Dialogs.EpisodeList -> {
            EpisodeListDialog(
                displayMode = episodeDisplayMode,
                currentEpisodeIndex = currentEpisodeIndex,
                episodeList = episodeList,
                dateRelativeTime = dateRelativeTime,
                dateFormat = dateFormat,
                onBookmarkClicked = onBookmarkClicked,
                onEpisodeClicked = onEpisodeClicked,
                onDismissRequest = onDismissRequest,
            )
        }
    }
}
