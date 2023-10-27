package eu.kanade.tachiyomi.ui.entries.manga.track

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.items.chapter.interactor.SyncChaptersWithTrackServiceTwoWay
import eu.kanade.domain.track.manga.model.toDbTrack
import eu.kanade.domain.track.manga.model.toDomainTrack
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.track.TrackDateSelector
import eu.kanade.presentation.track.TrackItemSelector
import eu.kanade.presentation.track.TrackScoreSelector
import eu.kanade.presentation.track.TrackStatusSelector
import eu.kanade.presentation.track.manga.MangaTrackInfoDialogHome
import eu.kanade.presentation.track.manga.MangaTrackServiceSearch
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.EnhancedMangaTrackService
import eu.kanade.tachiyomi.data.track.MangaTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.interactor.GetMangaWithChapters
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.domain.track.manga.interactor.DeleteMangaTrack
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack
import tachiyomi.domain.track.manga.model.MangaTrack
import tachiyomi.presentation.core.components.material.AlertDialogContent
import tachiyomi.presentation.core.components.material.padding
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import tachiyomi.domain.track.manga.model.MangaTrack as DbMangaTrack

data class MangaTrackInfoDialogHomeScreen(
    private val mangaId: Long,
    private val mangaTitle: String,
    private val sourceId: Long,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val sm = rememberScreenModel { Model(mangaId, sourceId) }

        val dateFormat = remember { UiPreferences.dateFormat(Injekt.get<UiPreferences>().dateFormat().get()) }
        val state by sm.state.collectAsState()

        MangaTrackInfoDialogHome(
            trackItems = state.trackItems,
            dateFormat = dateFormat,
            onStatusClick = {
                navigator.push(
                    TrackStatusSelectorScreen(
                        track = it.track!!,
                        serviceId = it.service.id,
                    ),
                )
            },
            onChapterClick = {
                navigator.push(
                    TrackChapterSelectorScreen(
                        track = it.track!!,
                        serviceId = it.service.id,
                    ),
                )
            },
            onScoreClick = {
                navigator.push(
                    TrackScoreSelectorScreen(
                        track = it.track!!,
                        serviceId = it.service.id,
                    ),
                )
            },
            onStartDateEdit = {
                navigator.push(
                    TrackDateSelectorScreen(
                        track = it.track!!,
                        serviceId = it.service.id,
                        start = true,
                    ),
                )
            },
            onEndDateEdit = {
                navigator.push(
                    TrackDateSelectorScreen(
                        track = it.track!!,
                        serviceId = it.service.id,
                        start = false,
                    ),
                )
            },
            onNewSearch = {
                if (it.service is EnhancedMangaTrackService) {
                    sm.registerEnhancedTracking(it)
                } else {
                    navigator.push(
                        TrackServiceSearchScreen(
                            mangaId = mangaId,
                            initialQuery = it.track?.title ?: mangaTitle,
                            currentUrl = it.track?.remoteUrl,
                            serviceId = it.service.id,
                        ),
                    )
                }
            },
            onOpenInBrowser = { openTrackerInBrowser(context, it) },
        ) { sm.unregisterTracking(it.service.id) }
    }

    /**
     * Opens registered tracker url in browser
     */
    private fun openTrackerInBrowser(context: Context, trackItem: MangaTrackItem) {
        val url = trackItem.track?.remoteUrl ?: return
        if (url.isNotBlank()) {
            context.openInBrowser(url)
        }
    }

    private class Model(
        private val mangaId: Long,
        private val sourceId: Long,
        private val getTracks: GetMangaTracks = Injekt.get(),
        private val deleteTrack: DeleteMangaTrack = Injekt.get(),
    ) : StateScreenModel<Model.State>(State()) {

        init {
            coroutineScope.launch {
                refreshTrackers()
            }

            coroutineScope.launch {
                getTracks.subscribe(mangaId)
                    .catch { logcat(LogPriority.ERROR, it) }
                    .distinctUntilChanged()
                    .map { it.mapToTrackItem() }
                    .collectLatest { trackItems -> mutableState.update { it.copy(trackItems = trackItems) } }
            }
        }

        fun registerEnhancedTracking(item: MangaTrackItem) {
            item.service as EnhancedMangaTrackService
            coroutineScope.launchNonCancellable {
                val manga = Injekt.get<GetManga>().await(mangaId) ?: return@launchNonCancellable
                try {
                    val matchResult = item.service.match(manga) ?: throw Exception()
                    item.service.mangaService.registerTracking(matchResult, mangaId)
                } catch (e: Exception) {
                    withUIContext { Injekt.get<Application>().toast(R.string.error_no_match) }
                }
            }
        }

        fun unregisterTracking(serviceId: Long) {
            coroutineScope.launchNonCancellable { deleteTrack.await(mangaId, serviceId) }
        }

        private suspend fun refreshTrackers() {
            val insertTrack = Injekt.get<InsertMangaTrack>()
            val getMangaWithChapters = Injekt.get<GetMangaWithChapters>()
            val syncTwoWayService = Injekt.get<SyncChaptersWithTrackServiceTwoWay>()
            val context = Injekt.get<Application>()

            try {
                val trackItems = getTracks.await(mangaId).mapToTrackItem()
                for (trackItem in trackItems) {
                    try {
                        val track = trackItem.track ?: continue
                        val domainMangaTrack = trackItem.service.mangaService.refresh(track.toDbTrack()).toDomainTrack() ?: continue
                        insertTrack.await(domainMangaTrack)

                        if (trackItem.service is EnhancedMangaTrackService) {
                            val allChapters = getMangaWithChapters.awaitChapters(mangaId)
                            syncTwoWayService.await(allChapters, domainMangaTrack, trackItem.service.mangaService)
                        }
                    } catch (e: Exception) {
                        logcat(
                            LogPriority.ERROR,
                            e,
                        ) { "Failed to refresh track data mangaId=$mangaId for service ${trackItem.service.id}" }
                        withUIContext {
                            context.toast(
                                context.getString(
                                    R.string.track_error,
                                    context.getString(trackItem.service.nameRes()),
                                    e.message,
                                ),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to refresh track data mangaId=$mangaId" }
                withUIContext { context.toast(e.message) }
            }
        }

        private fun List<MangaTrack>.mapToTrackItem(): List<MangaTrackItem> {
            val loggedServices = Injekt.get<TrackManager>().services.filter {
                it.isLogged && it is MangaTrackService
            }
            val source = Injekt.get<MangaSourceManager>().getOrStub(sourceId)
            return loggedServices
                // Map to TrackItem
                .map { service -> MangaTrackItem(find { it.syncId == service.id }, service) }
                // Show only if the service supports this manga's source
                .filter { (it.service as? EnhancedMangaTrackService)?.accept(source) ?: true }
        }

        data class State(
            val trackItems: List<MangaTrackItem> = emptyList(),
        )
    }
}

private data class TrackStatusSelectorScreen(
    private val track: DbMangaTrack,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm = rememberScreenModel {
            Model(
                track = track,
                service = Injekt.get<TrackManager>().getService(serviceId)!!,
            )
        }
        val state by sm.state.collectAsState()
        TrackStatusSelector(
            selection = state.selection,
            onSelectionChange = sm::setSelection,
            selections = remember { sm.getSelections() },
            onConfirm = { sm.setStatus(); navigator.pop() },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: DbMangaTrack,
        private val service: TrackService,
    ) : StateScreenModel<Model.State>(State(track.status.toInt())) {

        fun getSelections(): Map<Int, Int?> {
            return service.mangaService.getStatusListManga().associateWith { service.getStatus(it) }
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setStatus() {
            coroutineScope.launchNonCancellable {
                service.mangaService.setRemoteMangaStatus(track.toDbTrack(), state.value.selection)
            }
        }

        data class State(
            val selection: Int,
        )
    }
}

private data class TrackChapterSelectorScreen(
    private val track: DbMangaTrack,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm = rememberScreenModel {
            Model(
                track = track,
                service = Injekt.get<TrackManager>().getService(serviceId)!!,
            )
        }
        val state by sm.state.collectAsState()

        TrackItemSelector(
            selection = state.selection,
            onSelectionChange = sm::setSelection,
            range = remember { sm.getRange() },
            onConfirm = { sm.setChapter(); navigator.pop() },
            onDismissRequest = navigator::pop,
            isManga = true,
        )
    }

    private class Model(
        private val track: DbMangaTrack,
        private val service: TrackService,
    ) : StateScreenModel<Model.State>(State(track.lastChapterRead.toInt())) {

        fun getRange(): Iterable<Int> {
            val endRange = if (track.totalChapters > 0) {
                track.totalChapters
            } else {
                10000
            }
            return 0..endRange.toInt()
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setChapter() {
            coroutineScope.launchNonCancellable {
                service.mangaService.setRemoteLastChapterRead(track.toDbTrack(), state.value.selection)
            }
        }

        data class State(
            val selection: Int,
        )
    }
}

private data class TrackScoreSelectorScreen(
    private val track: DbMangaTrack,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm = rememberScreenModel {
            Model(
                track = track,
                service = Injekt.get<TrackManager>().getService(serviceId)!!,
            )
        }
        val state by sm.state.collectAsState()

        TrackScoreSelector(
            selection = state.selection,
            onSelectionChange = sm::setSelection,
            selections = remember { sm.getSelections() },
            onConfirm = { sm.setScore(); navigator.pop() },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: DbMangaTrack,
        private val service: TrackService,
    ) : StateScreenModel<Model.State>(State(service.mangaService.displayScore(track.toDbTrack()))) {

        fun getSelections(): List<String> {
            return service.mangaService.getScoreList()
        }

        fun setSelection(selection: String) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setScore() {
            coroutineScope.launchNonCancellable {
                service.mangaService.setRemoteScore(track.toDbTrack(), state.value.selection)
            }
        }

        data class State(
            val selection: String,
        )
    }
}

private data class TrackDateSelectorScreen(
    private val track: DbMangaTrack,
    private val serviceId: Long,
    private val start: Boolean,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm = rememberScreenModel {
            Model(
                track = track,
                service = Injekt.get<TrackManager>().getService(serviceId)!!,
                start = start,
            )
        }

        val canRemove = if (start) {
            track.startDate > 0
        } else {
            track.finishDate > 0
        }
        TrackDateSelector(
            title = if (start) {
                stringResource(R.string.track_started_reading_date)
            } else {
                stringResource(R.string.track_finished_reading_date)
            },
            initialSelectedDateMillis = sm.initialSelection,
            dateValidator = { utcMillis ->
                val dateToCheck = Instant.ofEpochMilli(utcMillis)
                    .atZone(ZoneOffset.systemDefault())
                    .toLocalDate()

                if (dateToCheck > LocalDate.now()) {
                    // Disallow future dates
                    return@TrackDateSelector false
                }

                if (start && track.finishDate > 0) {
                    // Disallow start date to be set later than finish date
                    val dateFinished = Instant.ofEpochMilli(track.finishDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    dateToCheck <= dateFinished
                } else if (!start && track.startDate > 0) {
                    // Disallow end date to be set earlier than start date
                    val dateStarted = Instant.ofEpochMilli(track.startDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    dateToCheck >= dateStarted
                } else {
                    // Nothing set before
                    true
                }
            },
            onConfirm = { sm.setDate(it); navigator.pop() },
            onRemove = { sm.confirmRemoveDate(navigator) }.takeIf { canRemove },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: DbMangaTrack,
        private val service: TrackService,
        private val start: Boolean,
    ) : ScreenModel {

        // In UTC
        val initialSelection: Long
            get() {
                val millis =
                    (if (start) track.startDate else track.finishDate)
                        .takeIf { it != 0L }
                        ?: Instant.now().toEpochMilli()
                return convertEpochMillisZone(millis, ZoneOffset.systemDefault(), ZoneOffset.UTC)
            }

        // In UTC
        fun setDate(millis: Long) {
            // Convert to local time
            val localMillis =
                convertEpochMillisZone(millis, ZoneOffset.UTC, ZoneOffset.systemDefault())
            coroutineScope.launchNonCancellable {
                if (start) {
                    service.mangaService.setRemoteStartDate(track.toDbTrack(), localMillis)
                } else {
                    service.mangaService.setRemoteFinishDate(track.toDbTrack(), localMillis)
                }
            }
        }

        fun confirmRemoveDate(navigator: Navigator) {
            navigator.push(TrackDateRemoverScreen(track, service.id, start))
        }
    }

    companion object {
        private fun convertEpochMillisZone(
            localMillis: Long,
            from: ZoneId,
            to: ZoneId,
        ): Long {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(localMillis), from)
                .atZone(to)
                .toInstant()
                .toEpochMilli()
        }
    }
}

private data class TrackDateRemoverScreen(
    private val track: DbMangaTrack,
    private val serviceId: Long,
    private val start: Boolean,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm = rememberScreenModel {
            Model(
                track = track,
                service = Injekt.get<TrackManager>().getService(serviceId)!!,
                start = start,
            )
        }
        AlertDialogContent(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.track_remove_date_conf_title),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                val serviceName = stringResource(sm.getServiceNameRes())
                Text(
                    text = if (start) {
                        stringResource(R.string.track_remove_start_date_conf_text, serviceName)
                    } else {
                        stringResource(R.string.track_remove_finish_date_conf_text, serviceName)
                    },
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small, Alignment.End),
                ) {
                    TextButton(onClick = navigator::pop) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                    FilledTonalButton(
                        onClick = { sm.removeDate(); navigator.popUntil { it is MangaTrackInfoDialogHomeScreen } },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(text = stringResource(R.string.action_remove))
                    }
                }
            },
        )
    }

    private class Model(
        private val track: DbMangaTrack,
        private val service: TrackService,
        private val start: Boolean,
    ) : ScreenModel {

        fun getServiceNameRes() = service.nameRes()

        fun removeDate() {
            coroutineScope.launchNonCancellable {
                if (start) {
                    service.mangaService.setRemoteStartDate(track.toDbTrack(), 0)
                } else {
                    service.mangaService.setRemoteFinishDate(track.toDbTrack(), 0)
                }
            }
        }
    }
}

data class TrackServiceSearchScreen(
    private val mangaId: Long,
    private val initialQuery: String,
    private val currentUrl: String?,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm = rememberScreenModel {
            Model(
                mangaId = mangaId,
                currentUrl = currentUrl,
                initialQuery = initialQuery,
                service = Injekt.get<TrackManager>().getService(serviceId)!!,
            )
        }

        val state by sm.state.collectAsState()

        var textFieldValue by remember { mutableStateOf(TextFieldValue(initialQuery)) }
        MangaTrackServiceSearch(
            query = textFieldValue,
            onQueryChange = { textFieldValue = it },
            onDispatchQuery = { sm.trackingSearch(textFieldValue.text) },
            queryResult = state.queryResult,
            selected = state.selected,
            onSelectedChange = sm::updateSelection,
            onConfirmSelection = { sm.registerTracking(state.selected!!); navigator.pop() },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val mangaId: Long,
        private val currentUrl: String? = null,
        initialQuery: String,
        private val service: TrackService,
    ) : StateScreenModel<Model.State>(State()) {

        init {
            // Run search on first launch
            if (initialQuery.isNotBlank()) {
                trackingSearch(initialQuery)
            }
        }

        fun trackingSearch(query: String) {
            coroutineScope.launch {
                // To show loading state
                mutableState.update { it.copy(queryResult = null, selected = null) }

                val result = withIOContext {
                    try {
                        val results = service.mangaService.searchManga(query)
                        Result.success(results)
                    } catch (e: Throwable) {
                        Result.failure(e)
                    }
                }
                mutableState.update { oldState ->
                    oldState.copy(
                        queryResult = result,
                        selected = result.getOrNull()?.find { it.tracking_url == currentUrl },
                    )
                }
            }
        }

        fun registerTracking(item: MangaTrackSearch) {
            coroutineScope.launchNonCancellable { service.mangaService.registerTracking(item, mangaId) }
        }

        fun updateSelection(selected: MangaTrackSearch) {
            mutableState.update { it.copy(selected = selected) }
        }

        data class State(
            val queryResult: Result<List<MangaTrackSearch>>? = null,
            val selected: MangaTrackSearch? = null,
        )
    }
}
