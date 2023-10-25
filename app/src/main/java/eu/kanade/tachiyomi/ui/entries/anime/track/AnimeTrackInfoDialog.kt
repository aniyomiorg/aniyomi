package eu.kanade.tachiyomi.ui.entries.anime.track

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
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.model.toDomainTrack
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.track.TrackDateSelector
import eu.kanade.presentation.track.TrackItemSelector
import eu.kanade.presentation.track.TrackScoreSelector
import eu.kanade.presentation.track.TrackStatusSelector
import eu.kanade.presentation.track.anime.AnimeTrackInfoDialogHome
import eu.kanade.presentation.track.anime.AnimeTrackServiceSearch
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.AnimeTrackService
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
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
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.GetAnimeWithEpisodes
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.track.anime.interactor.DeleteAnimeTrack
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.presentation.core.components.material.AlertDialogContent
import tachiyomi.presentation.core.components.material.padding
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import tachiyomi.domain.track.anime.model.AnimeTrack as DbAnimeTrack

data class AnimeTrackInfoDialogHomeScreen(
    private val animeId: Long,
    private val animeTitle: String,
    private val sourceId: Long,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val sm = rememberScreenModel { Model(animeId, sourceId) }

        val dateFormat = remember { UiPreferences.dateFormat(Injekt.get<UiPreferences>().dateFormat().get()) }
        val state by sm.state.collectAsState()

        AnimeTrackInfoDialogHome(
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
            onEpisodeClick = {
                navigator.push(
                    TrackEpisodeSelectorScreen(
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
                if (it.service is EnhancedAnimeTrackService) {
                    sm.registerEnhancedTracking(it)
                } else {
                    navigator.push(
                        TrackServiceSearchScreen(
                            animeId = animeId,
                            initialQuery = it.track?.title ?: animeTitle,
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
    private fun openTrackerInBrowser(context: Context, trackItem: AnimeTrackItem) {
        val url = trackItem.track?.remoteUrl ?: return
        if (url.isNotBlank()) {
            context.openInBrowser(url)
        }
    }

    private class Model(
        private val animeId: Long,
        private val sourceId: Long,
        private val getTracks: GetAnimeTracks = Injekt.get(),
        private val deleteTrack: DeleteAnimeTrack = Injekt.get(),
    ) : StateScreenModel<Model.State>(State()) {

        init {
            coroutineScope.launch {
                refreshTrackers()
            }

            coroutineScope.launch {
                getTracks.subscribe(animeId)
                    .catch { logcat(LogPriority.ERROR, it) }
                    .distinctUntilChanged()
                    .map { it.mapToTrackItem() }
                    .collectLatest { trackItems -> mutableState.update { it.copy(trackItems = trackItems) } }
            }
        }

        fun registerEnhancedTracking(item: AnimeTrackItem) {
            item.service as EnhancedAnimeTrackService
            coroutineScope.launchNonCancellable {
                val anime = Injekt.get<GetAnime>().await(animeId) ?: return@launchNonCancellable
                try {
                    val matchResult = item.service.match(anime) ?: throw Exception()
                    item.service.animeService.registerTracking(matchResult, animeId)
                } catch (e: Exception) {
                    withUIContext { Injekt.get<Application>().toast(R.string.error_no_match) }
                }
            }
        }

        fun unregisterTracking(serviceId: Long) {
            coroutineScope.launchNonCancellable { deleteTrack.await(animeId, serviceId) }
        }

        private suspend fun refreshTrackers() {
            val insertAnimeTrack = Injekt.get<InsertAnimeTrack>()
            val getAnimeWithEpisodes = Injekt.get<GetAnimeWithEpisodes>()
            val syncTwoWayService = Injekt.get<SyncEpisodesWithTrackServiceTwoWay>()
            val context = Injekt.get<Application>()

            try {
                val trackItems = getTracks.await(animeId).mapToTrackItem()
                for (trackItem in trackItems) {
                    try {
                        val track = trackItem.track ?: continue
                        val domainAnimeTrack = trackItem.service.animeService.refresh(track.toDbTrack()).toDomainTrack() ?: continue
                        insertAnimeTrack.await(domainAnimeTrack)

                        if (trackItem.service is EnhancedAnimeTrackService) {
                            val allEpisodes = getAnimeWithEpisodes.awaitEpisodes(animeId)
                            syncTwoWayService.await(allEpisodes, domainAnimeTrack, trackItem.service.animeService)
                        }
                    } catch (e: Exception) {
                        logcat(
                            LogPriority.ERROR,
                            e,
                        ) { "Failed to refresh track data mangaId=$animeId for service ${trackItem.service.id}" }
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
                logcat(LogPriority.ERROR, e) { "Failed to refresh track data animeId=$animeId" }
                withUIContext { context.toast(e.message) }
            }
        }

        private fun List<AnimeTrack>.mapToTrackItem(): List<AnimeTrackItem> {
            val dbTracks = map { it.toDbTrack() }
            val loggedServices = Injekt.get<TrackManager>().services.filter {
                it.isLogged && it is AnimeTrackService
            }
            val source = Injekt.get<AnimeSourceManager>().getOrStub(sourceId)
            return loggedServices
                // Map to TrackItem
                .map { service -> AnimeTrackItem(find { it.syncId.toLong() == service.id }, service) }
                // Show only if the service supports this anime's source
                .filter { (it.service as? EnhancedAnimeTrackService)?.accept(source) ?: true }
        }

        data class State(
            val trackItems: List<AnimeTrackItem> = emptyList(),
        )
    }
}

private data class TrackStatusSelectorScreen(
    private val track: DbAnimeTrack,
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
        private val track: DbAnimeTrack,
        private val service: TrackService,
    ) : StateScreenModel<Model.State>(State(track.status.toInt())) {

        fun getSelections(): Map<Int, Int?> {
            return service.animeService.getStatusListAnime().associateWith { service.getStatus(it) }
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setStatus() {
            coroutineScope.launchNonCancellable {
                service.animeService.setRemoteAnimeStatus(track.toDbTrack(), state.value.selection)
            }
        }

        data class State(
            val selection: Int,
        )
    }
}

private data class TrackEpisodeSelectorScreen(
    private val track: DbAnimeTrack,
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
            onConfirm = { sm.setEpisode(); navigator.pop() },
            onDismissRequest = navigator::pop,
            isManga = false,
        )
    }

    private class Model(
        private val track: DbAnimeTrack,
        private val service: TrackService,
    ) : StateScreenModel<Model.State>(State(track.lastEpisodeSeen.toInt())) {

        fun getRange(): Iterable<Int> {
            val endRange = if (track.totalEpisodes > 0) {
                track.totalEpisodes
            } else {
                10000
            }
            return 0..endRange.toInt()
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setEpisode() {
            coroutineScope.launchNonCancellable {
                service.animeService.setRemoteLastEpisodeSeen(track.toDbTrack(), state.value.selection)
            }
        }

        data class State(
            val selection: Int,
        )
    }
}

private data class TrackScoreSelectorScreen(
    private val track: DbAnimeTrack,
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
        private val track: DbAnimeTrack,
        private val service: TrackService,
    ) : StateScreenModel<Model.State>(State(service.animeService.displayScore(track.toDbTrack()))) {

        fun getSelections(): List<String> {
            return service.animeService.getScoreList()
        }

        fun setSelection(selection: String) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setScore() {
            coroutineScope.launchNonCancellable {
                service.animeService.setRemoteScore(track.toDbTrack(), state.value.selection)
            }
        }

        data class State(
            val selection: String,
        )
    }
}

private data class TrackDateSelectorScreen(
    private val track: DbAnimeTrack,
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
        private val track: DbAnimeTrack,
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
                    service.animeService.setRemoteStartDate(track.toDbTrack(), localMillis)
                } else {
                    service.animeService.setRemoteFinishDate(track.toDbTrack(), localMillis)
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
    private val track: DbAnimeTrack,
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
                        onClick = { sm.removeDate(); navigator.popUntil { it is AnimeTrackInfoDialogHomeScreen } },
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
        private val track: DbAnimeTrack,
        private val service: TrackService,
        private val start: Boolean,
    ) : ScreenModel {

        fun getServiceNameRes() = service.nameRes()

        fun removeDate() {
            coroutineScope.launchNonCancellable {
                if (start) {
                    service.animeService.setRemoteStartDate(track.toDbTrack(), 0)
                } else {
                    service.animeService.setRemoteFinishDate(track.toDbTrack(), 0)
                }
            }
        }
    }
}

data class TrackServiceSearchScreen(
    private val animeId: Long,
    private val initialQuery: String,
    private val currentUrl: String?,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm = rememberScreenModel {
            Model(
                animeId = animeId,
                currentUrl = currentUrl,
                initialQuery = initialQuery,
                service = Injekt.get<TrackManager>().getService(serviceId)!!,
            )
        }

        val state by sm.state.collectAsState()

        var textFieldValue by remember { mutableStateOf(TextFieldValue(initialQuery)) }
        AnimeTrackServiceSearch(
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
        private val animeId: Long,
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
                        val results = service.animeService.searchAnime(query)
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

        fun registerTracking(item: AnimeTrackSearch) {
            coroutineScope.launchNonCancellable { service.animeService.registerTracking(item, animeId) }
        }

        fun updateSelection(selected: AnimeTrackSearch) {
            mutableState.update { it.copy(selected = selected) }
        }

        data class State(
            val queryResult: Result<List<AnimeTrackSearch>>? = null,
            val selected: AnimeTrackSearch? = null,
        )
    }
}
