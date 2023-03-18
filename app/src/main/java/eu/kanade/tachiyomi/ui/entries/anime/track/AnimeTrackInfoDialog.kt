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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.entries.anime.interactor.GetAnime
import eu.kanade.domain.entries.anime.interactor.GetAnimeWithEpisodes
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.track.anime.interactor.DeleteAnimeTrack
import eu.kanade.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.domain.track.anime.interactor.InsertAnimeTrack
import eu.kanade.domain.track.anime.model.AnimeTrack
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.model.toDomainTrack
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AlertDialogContent
import eu.kanade.presentation.entries.TrackDateSelector
import eu.kanade.presentation.entries.TrackItemSelector
import eu.kanade.presentation.entries.TrackScoreSelector
import eu.kanade.presentation.entries.TrackStatusSelector
import eu.kanade.presentation.entries.anime.AnimeTrackInfoDialogHome
import eu.kanade.presentation.entries.anime.AnimeTrackServiceSearch
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack as DbAnimeTrack

data class AnimeTrackInfoDialogHomeScreen(
    private val animeId: Long,
    private val animeTitle: String,
    private val sourceId: Long,
) : Screen {
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
                            currentUrl = it.track?.tracking_url,
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
        val url = trackItem.track?.tracking_url ?: return
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
            // Refresh data
            coroutineScope.launch {
                try {
                    val trackItems = getTracks.await(animeId).mapToTrackItem()
                    val insertTrack = Injekt.get<InsertAnimeTrack>()
                    val getAnimeWithEpisodes = Injekt.get<GetAnimeWithEpisodes>()
                    val syncTwoWayService = Injekt.get<SyncEpisodesWithTrackServiceTwoWay>()
                    trackItems.forEach {
                        val track = it.track ?: return@forEach
                        val domainTrack = it.service.animeService.refresh(track).toDomainTrack() ?: return@forEach
                        insertTrack.await(domainTrack)

                        if (it.service is EnhancedAnimeTrackService) {
                            val allEpisodes = getAnimeWithEpisodes.awaitEpisodes(animeId)
                            syncTwoWayService.await(allEpisodes, domainTrack, it.service.animeService)
                        }
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to refresh track data animeId=$animeId" }
                    withUIContext { Injekt.get<Application>().toast(e.message) }
                }
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

        private fun List<AnimeTrack>.mapToTrackItem(): List<AnimeTrackItem> {
            val dbTracks = map { it.toDbTrack() }
            val loggedServices = Injekt.get<TrackManager>().services.filter { it.isLogged }
            val source = Injekt.get<AnimeSourceManager>().getOrStub(sourceId)
            return loggedServices
                // Map to TrackItem
                .map { service -> AnimeTrackItem(dbTracks.find { it.sync_id.toLong() == service.id }, service) }
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
) : Screen {

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
    ) : StateScreenModel<Model.State>(State(track.status)) {

        fun getSelections(): Map<Int, String> {
            return service.animeService.getStatusListAnime().associateWith { service.getStatus(it) }
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setStatus() {
            coroutineScope.launchNonCancellable {
                service.animeService.setRemoteAnimeStatus(track, state.value.selection)
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
) : Screen {

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
    ) : StateScreenModel<Model.State>(State(track.last_episode_seen.toInt())) {

        fun getRange(): Iterable<Int> {
            val endRange = if (track.total_episodes > 0) {
                track.total_episodes
            } else {
                10000
            }
            return 0..endRange
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setEpisode() {
            coroutineScope.launchNonCancellable {
                service.animeService.setRemoteLastEpisodeSeen(track, state.value.selection)
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
) : Screen {

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
    ) : StateScreenModel<Model.State>(State(service.animeService.displayScore(track))) {

        fun getSelections(): List<String> {
            return service.animeService.getScoreList()
        }

        fun setSelection(selection: String) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setScore() {
            coroutineScope.launchNonCancellable {
                service.animeService.setRemoteScore(track, state.value.selection)
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
) : Screen {

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
        val state by sm.state.collectAsState()

        val canRemove = if (start) {
            track.started_watching_date > 0
        } else {
            track.finished_watching_date > 0
        }
        TrackDateSelector(
            title = if (start) {
                stringResource(R.string.track_started_reading_date)
            } else {
                stringResource(R.string.track_finished_reading_date)
            },
            selection = state.selection,
            onSelectionChange = sm::setSelection,
            onConfirm = { sm.setDate(); navigator.pop() },
            onRemove = { sm.confirmRemoveDate(navigator) }.takeIf { canRemove },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: DbAnimeTrack,
        private val service: TrackService,
        private val start: Boolean,
    ) : StateScreenModel<Model.State>(
        State(
            (if (start) track.started_watching_date else track.finished_watching_date)
                .takeIf { it != 0L }
                ?.let {
                    Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                ?: LocalDate.now(),
        ),
    ) {

        fun setSelection(selection: LocalDate) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setDate() {
            coroutineScope.launchNonCancellable {
                val millis = state.value.selection.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (start) {
                    service.animeService.setRemoteStartDate(track, millis)
                } else {
                    service.animeService.setRemoteFinishDate(track, millis)
                }
            }
        }

        fun confirmRemoveDate(navigator: Navigator) {
            navigator.push(TrackDateRemoverScreen(track, service.id, start))
        }

        data class State(
            val selection: LocalDate,
        )
    }
}

private data class TrackDateRemoverScreen(
    private val track: DbAnimeTrack,
    private val serviceId: Long,
    private val start: Boolean,
) : Screen {

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
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
                    service.animeService.setRemoteStartDate(track, 0)
                } else {
                    service.animeService.setRemoteFinishDate(track, 0)
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
) : Screen {

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

        fun registerTracking(item: DbAnimeTrack) {
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
