package eu.kanade.tachiyomi.ui.recent.animehistory

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.anime.interactor.GetAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.animehistory.interactor.DeleteAllAnimeHistory
import eu.kanade.domain.animehistory.interactor.GetAnimeHistory
import eu.kanade.domain.animehistory.interactor.GetNextEpisode
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryByAnimeId
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryById
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.presentation.animehistory.AnimeHistoryUiModel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.anime.AnimeController.Companion.EXT_ANIME
import eu.kanade.tachiyomi.ui.anime.AnimeController.Companion.EXT_EPISODE
import eu.kanade.tachiyomi.ui.player.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class AnimeHistoryPresenter(
    private val presenterScope: CoroutineScope,
    private val state: AnimeHistoryStateImpl = AnimeHistoryState() as AnimeHistoryStateImpl,
    private val getAnime: GetAnime = Injekt.get(),
    private val getAnimeHistory: GetAnimeHistory = Injekt.get(),
    private val getNextEpisode: GetNextEpisode = Injekt.get(),
    private val deleteAllAnimeHistory: DeleteAllAnimeHistory = Injekt.get(),
    private val removeAnimeHistoryById: RemoveAnimeHistoryById = Injekt.get(),
    private val removeAnimeHistoryByAnimeId: RemoveAnimeHistoryByAnimeId = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
    playerPreferences: PlayerPreferences = Injekt.get(),
) : AnimeHistoryState by state {

    var context: Context? = null

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    val isDownloadOnly = preferences.downloadedOnly().get()

    val isIncognitoMode = preferences.incognitoMode().get()

    val useExternalPlayer = playerPreferences.alwaysUseExternalPlayer().get()

    fun onCreate(context: Context?) {
        this.context = context
    }

    @Composable
    fun getAnimeHistory(): Flow<List<AnimeHistoryUiModel>> {
        val query = searchQuery ?: ""
        return remember(query) {
            getAnimeHistory.subscribe(query)
                .distinctUntilChanged()
                .catch { error ->
                    logcat(LogPriority.ERROR, error)
                    _events.send(Event.InternalError)
                }
                .map { pagingData ->
                    pagingData.toAnimeHistoryUiModels()
                }
        }
    }

    private fun List<AnimeHistoryWithRelations>.toAnimeHistoryUiModels(): List<AnimeHistoryUiModel> {
        return map { AnimeHistoryUiModel.Item(it) }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                val afterDate = after?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                when {
                    beforeDate.time != afterDate.time && afterDate.time != 0L -> AnimeHistoryUiModel.Header(afterDate)
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }

    fun removeFromHistory(history: AnimeHistoryWithRelations) {
        presenterScope.launchIO {
            removeAnimeHistoryById.await(history)
        }
    }

    fun removeAllFromHistory(animeId: Long) {
        presenterScope.launchIO {
            removeAnimeHistoryByAnimeId.await(animeId)
        }
    }

    fun getNextEpisodeForAnime(animeId: Long, episodeId: Long) {
        presenterScope.launchIO {
            val episode = getNextEpisode.await(animeId, episodeId)!!
            _events.send(Event.OpenEpisode(episode))
        }
    }

    fun deleteAllAnimeHistory() {
        presenterScope.launchIO {
            val result = deleteAllAnimeHistory.await()
            if (!result) return@launchIO
            withUIContext {
                context?.toast(R.string.clear_history_completed)
            }
        }
    }

    fun resumeLastEpisodeSeen() {
        presenterScope.launchIO {
            val episode = getNextEpisode.await()
            _events.send(if (episode != null) Event.OpenEpisode(episode) else Event.NoNextEpisodeFound)
        }
    }

    fun openEpisode(episode: Episode, context: Context) {
        presenterScope.launchNonCancellable {
            val anime = getAnime.await(episode.animeId) ?: return@launchNonCancellable
            val source = sourceManager.get(anime.source) ?: return@launchNonCancellable

            if (useExternalPlayer) {
                openEpisodeExternal(episode, anime, source, context)
            } else {
                openEpisodeInternal(episode, context)
            }
        }
    }

    private fun openEpisodeInternal(episode: Episode, context: Context) {
        context.startActivity(PlayerActivity.newIntent(context, episode.animeId, episode.id))
    }

    private fun openEpisodeExternal(episode: Episode, anime: Anime, source: AnimeSource, context: Context) {
        launchIO {
            val video = try {
                EpisodeLoader.getLink(episode.toDbEpisode(), anime.toDbAnime(), source)
                    .awaitSingle()
            } catch (e: Exception) {
                launchUI { context.toast(e.message) }
                return@launchIO
            }
            if (video != null) {
                EXT_EPISODE = episode
                EXT_ANIME = anime

                val extIntent = ExternalIntents(anime, source).getExternalIntent(
                    episode,
                    video,
                    context,
                )
                if (extIntent != null) {
                    try {
                        val activity = context as Activity
                        ActivityCompat.startActivityForResult(
                            activity,
                            extIntent,
                            AnimeController.REQUEST_EXTERNAL,
                            null,
                        )
                    } catch (e: Exception) {
                        launchUI { context.toast(e.message) }
                        return@launchIO
                    }
                }
            } else {
                launchUI { context.toast("Couldn't find any video links.") }
                return@launchIO
            }
        }
    }

    sealed class Dialog {
        object DeleteAll : Dialog()
        data class Delete(val history: AnimeHistoryWithRelations) : Dialog()
    }

    sealed class Event {
        object InternalError : Event()
        object NoNextEpisodeFound : Event()
        data class OpenEpisode(val episode: Episode) : Event()
    }
}

@Stable
interface AnimeHistoryState {
    var searchQuery: String?
    var dialog: AnimeHistoryPresenter.Dialog?
}

fun AnimeHistoryState(): AnimeHistoryState {
    return AnimeHistoryStateImpl()
}
class AnimeHistoryStateImpl : AnimeHistoryState {
    override var searchQuery: String? by mutableStateOf(null)
    override var dialog: AnimeHistoryPresenter.Dialog? by mutableStateOf(null)
}
