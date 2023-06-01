package eu.kanade.presentation.entries.anime

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.core.util.asFlow
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.util.padding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.ui.player.loader.EpisodeLoader
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.lang.launchUI
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.interactor.GetEpisode
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EpisodeOptionsDialogScreen(
    private val episodeId: Long,
    private val animeId: Long,
    private val sourceId: Long,
    private val useExternalDownloader: Boolean,
) : Screen {

    @Composable
    override fun Content() {
        val sm = rememberScreenModel {
            EpisodeOptionsDialogScreenModel(
                episodeId = episodeId,
                animeId = animeId,
                sourceId = sourceId,
            )
        }
        val state by sm.state.collectAsState()

        EpisodeOptionsDialog(
            episode = state.episode,
            anime = state.anime,
            resultList = state.resultList,
            // onDismissRequest = onDismissRequest,
        )
    }

    class EpisodeOptionsDialogScreenModel(
        episodeId: Long,
        animeId: Long,
        sourceId: Long,
    ) : StateScreenModel<State>(State()) {
        private val sourceManager: AnimeSourceManager = Injekt.get()

        init {
            coroutineScope.launch {
                // To show loading state
                mutableState.update { it.copy(episode = null, anime = null, resultList = null) }

                val episode = Injekt.get<GetEpisode>().await(episodeId)!!
                val anime = Injekt.get<GetAnime>().await(animeId)!!
                val source = sourceManager.getOrStub(sourceId)

                val result = withIOContext {
                    try {
                        val results =
                            EpisodeLoader.getLinks(episode, anime, source).asFlow().first()
                        Result.success(results)
                    } catch (e: Throwable) {
                        Result.failure(e)
                    }
                }

                mutableState.update { it.copy(episode = episode, anime = anime, resultList = result) }
            }
        }
    }

    @Immutable
    data class State(
        val episode: Episode? = null,
        val anime: Anime? = null,
        val resultList: Result<List<Video>>? = null,
    )

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    fun EpisodeOptionsDialog(
        episode: Episode?,
        anime: Anime?,
        resultList: Result<List<Video>>? = null,
    ) {
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val copiedString = stringResource(R.string.copied_video_link_to_clipboard)

        val downloadManager = Injekt.get<AnimeDownloadManager>()

        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .windowInsetsPadding(WindowInsets.systemBars),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            QualityItem(
                label = stringResource(R.string.choose_video_quality),
                enabled = false,
                useExternalDownloader = false,
            )
            if (resultList == null) {
                LoadingScreen()
            } else {
                val videoList = resultList.getOrNull()
                if (!videoList.isNullOrEmpty()) {
                    videoList.forEach { video ->
                        val downloadEpisode: (Boolean) -> Unit = {
                            downloadManager.downloadEpisodes(anime!!, listOf(episode!!), true, it, video)
                            onDismissEpisodeOptionsDialogScreen()
                        }
                        QualityItem(
                            label = video.quality,
                            enabled = true,
                            useExternalDownloader = useExternalDownloader,
                            onQualityClick = { downloadEpisode(false) },
                            onAlternateClick = { downloadEpisode(true) },
                            onCopyClick = {
                                if (video.videoUrl != null) {
                                    clipboardManager.setText(AnnotatedString(video.videoUrl!!))
                                    scope.launch { context.toast(copiedString) }
                                }
                            },
                        )
                    }
                } else {
                    logcat(LogPriority.ERROR) { "Error getting links" }
                    launchUI { context.toast("Video list is missing") }
                    onDismissEpisodeOptionsDialogScreen()
                }
            }
        }
    }
}

var onDismissEpisodeOptionsDialogScreen: () -> Unit = {}

@Composable
private fun QualityItem(
    label: String,
    enabled: Boolean,
    useExternalDownloader: Boolean,
    onQualityClick: () -> Unit = {},
    onAlternateClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = onQualityClick,
            )
            .padding(horizontal = TabbedDialogPaddings.Horizontal, vertical = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (enabled) {
            IconButton(
                onClick = onCopyClick,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.copy),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val icon = if (useExternalDownloader) Icons.Outlined.Download else Icons.Outlined.SystemUpdateAlt
            val description = if (useExternalDownloader) R.string.action_start_download_internally else R.string.action_start_download_externally
            IconButton(
                onClick = onAlternateClick,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
