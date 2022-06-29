package eu.kanade.tachiyomi.ui.anime.episode

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import eu.kanade.presentation.components.EpisodeDownloadIndicator
import eu.kanade.presentation.manga.EpisodeDownloadAction
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.data.download.model.AnimeDownload

class EpisodeDownloadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {

    private var state by mutableStateOf(AnimeDownload.State.NOT_DOWNLOADED)
    private var progress by mutableStateOf(0)

    var listener: (EpisodeDownloadAction) -> Unit = {}

    @Composable
    override fun Content() {
        TachiyomiTheme {
            EpisodeDownloadIndicator(
                downloadState = state,
                downloadProgress = progress,
                onClick = listener,
            )
        }
    }

    fun setState(state: AnimeDownload.State, progress: Int = 0) {
        this.state = state
        this.progress = progress
    }
}
