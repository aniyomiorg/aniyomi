package eu.kanade.tachiyomi.ui.history.manga

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.util.insertSeparators
import eu.kanade.presentation.history.manga.MangaHistoryUiModel
import eu.kanade.tachiyomi.util.lang.toDateKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.history.manga.interactor.GetMangaHistory
import tachiyomi.domain.history.manga.interactor.GetNextChapters
import tachiyomi.domain.history.manga.interactor.RemoveMangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.items.chapter.model.Chapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class MangaHistoryScreenModel(
    private val getHistory: GetMangaHistory = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val removeHistory: RemoveMangaHistory = Injekt.get(),
) : StateScreenModel<HistoryState>(HistoryState()) {

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val _query: MutableStateFlow<String?> = MutableStateFlow(null)
    val query: StateFlow<String?> = _query.asStateFlow()

    init {
        coroutineScope.launch {
            _query.collectLatest { query ->
                getHistory.subscribe(query ?: "")
                    .distinctUntilChanged()
                    .catch { error ->
                        logcat(LogPriority.ERROR, error)
                        _events.send(Event.InternalError)
                    }
                    .map { it.toHistoryUiModels() }
                    .flowOn(Dispatchers.IO)
                    .collect { newList -> mutableState.update { it.copy(list = newList) } }
            }
        }
    }

    fun search(query: String?) {
        coroutineScope.launchIO {
            _query.emit(query)
        }
    }

    private fun List<MangaHistoryWithRelations>.toHistoryUiModels(): List<MangaHistoryUiModel> {
        return map { MangaHistoryUiModel.Item(it) }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.readAt?.time?.toDateKey() ?: Date(0)
                val afterDate = after?.item?.readAt?.time?.toDateKey() ?: Date(0)
                when {
                    beforeDate.time != afterDate.time && afterDate.time != 0L -> MangaHistoryUiModel.Header(afterDate)
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }

    suspend fun getNextChapter(): Chapter? {
        return withIOContext { getNextChapters.await(onlyUnread = false).firstOrNull() }
    }

    fun getNextChapterForManga(mangaId: Long, chapterId: Long) {
        coroutineScope.launchIO {
            sendNextChapterEvent(getNextChapters.await(mangaId, chapterId, onlyUnread = false))
        }
    }

    private suspend fun sendNextChapterEvent(chapters: List<Chapter>) {
        val chapter = chapters.firstOrNull()
        _events.send(Event.OpenChapter(chapter))
    }

    fun removeFromHistory(history: MangaHistoryWithRelations) {
        coroutineScope.launchIO {
            removeHistory.await(history)
        }
    }

    fun removeAllFromHistory(mangaId: Long) {
        coroutineScope.launchIO {
            removeHistory.await(mangaId)
        }
    }

    fun removeAllHistory() {
        coroutineScope.launchIO {
            val result = removeHistory.awaitAll()
            if (!result) return@launchIO
            _events.send(Event.HistoryCleared)
        }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    sealed class Dialog {
        object DeleteAll : Dialog()
        data class Delete(val history: MangaHistoryWithRelations) : Dialog()
    }

    sealed class Event {
        data class OpenChapter(val chapter: Chapter?) : Event()
        object InternalError : Event()
        object HistoryCleared : Event()
    }
}

@Immutable
data class HistoryState(
    val list: List<MangaHistoryUiModel>? = null,
    val dialog: MangaHistoryScreenModel.Dialog? = null,
)
