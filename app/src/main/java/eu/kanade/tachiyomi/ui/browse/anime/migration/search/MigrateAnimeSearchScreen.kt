package eu.kanade.tachiyomi.ui.browse.anime.migration.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEachIndexed
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.category.anime.interactor.GetAnimeCategories
import eu.kanade.domain.category.anime.interactor.SetAnimeCategories
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.entries.anime.model.AnimeUpdate
import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.domain.items.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.items.episode.interactor.UpdateEpisode
import eu.kanade.domain.items.episode.model.toEpisodeUpdate
import eu.kanade.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.domain.track.anime.interactor.InsertAnimeTrack
import eu.kanade.presentation.browse.anime.MigrateAnimeSearchScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.ui.browse.anime.migration.AnimeMigrationFlags
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class MigrateAnimeSearchScreen(private val animeId: Long) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MigrateAnimeSearchScreenModel(animeId = animeId) }
        val state by screenModel.state.collectAsState()

        MigrateAnimeSearchScreen(
            navigateUp = navigator::pop,
            state = state,
            getAnime = { source, anime ->
                screenModel.getAnime(source = source, initialAnime = anime)
            },
            onChangeSearchQuery = screenModel::updateSearchQuery,
            onSearch = screenModel::search,
            onClickSource = {
                if (!screenModel.incognitoMode.get()) {
                    screenModel.lastUsedSourceId.set(it.id)
                }
                navigator.push(AnimeSourceSearchScreen(state.anime!!, it.id, state.searchQuery))
            },
            onClickItem = { screenModel.setDialog(MigrateAnimeSearchDialog.Migrate(it)) },
            onLongClickItem = { navigator.push(AnimeScreen(it.id, true)) },
        )

        when (val dialog = state.dialog) {
            null -> {}
            is MigrateAnimeSearchDialog.Migrate -> {
                MigrateAnimeDialog(
                    oldAnime = state.anime!!,
                    newAnime = dialog.anime,
                    screenModel = rememberScreenModel { MigrateAnimeDialogScreenModel() },
                    onDismissRequest = { screenModel.setDialog(null) },
                    onClickTitle = {
                        navigator.push(AnimeScreen(dialog.anime.id, true))
                    },
                    onPopScreen = {
                        if (navigator.lastItem is AnimeScreen) {
                            val lastItem = navigator.lastItem
                            navigator.popUntil { navigator.items.contains(lastItem) }
                            navigator.push(AnimeScreen(dialog.anime.id))
                        } else {
                            navigator.replace(AnimeScreen(dialog.anime.id))
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun MigrateAnimeDialog(
    oldAnime: Anime,
    newAnime: Anime,
    screenModel: MigrateAnimeDialogScreenModel,
    onDismissRequest: () -> Unit,
    onClickTitle: () -> Unit,
    onPopScreen: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeFlags = remember { AnimeMigrationFlags.getEnabledFlagsPositions(screenModel.migrateFlags.get()) }
    val items = remember {
        AnimeMigrationFlags.titles(oldAnime)
            .map { context.getString(it) }
            .toList()
    }
    val selected = remember {
        mutableStateListOf(*List(items.size) { i -> activeFlags.contains(i) }.toTypedArray())
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.migration_dialog_what_to_include))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                items.forEachIndexed { index, title ->
                    val onChange: () -> Unit = {
                        selected[index] = !selected[index]
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onChange),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = selected[index], onCheckedChange = { onChange() })
                        Text(text = title)
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    onClickTitle()
                    onDismissRequest()
                },) {
                    Text(text = stringResource(R.string.action_show_anime))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    scope.launchIO {
                        screenModel.migrateAnime(oldAnime, newAnime, false)
                        launchUI {
                            onPopScreen()
                        }
                    }
                },) {
                    Text(text = stringResource(R.string.copy))
                }
                TextButton(onClick = {
                    scope.launchIO {
                        val selectedIndices = mutableListOf<Int>()
                        selected.fastForEachIndexed { i, b -> if (b) selectedIndices.add(i) }
                        val newValue = AnimeMigrationFlags.getFlagsFromPositions(selectedIndices.toTypedArray())
                        screenModel.migrateFlags.set(newValue)
                        screenModel.migrateAnime(oldAnime, newAnime, true)
                        launchUI {
                            onPopScreen()
                        }
                    }
                },) {
                    Text(text = stringResource(R.string.migrate))
                }
            }
        },
    )
}

class MigrateAnimeDialogScreenModel(
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val syncEpisodesWithAnimeSource: SyncEpisodesWithSource = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
) : ScreenModel {

    val migrateFlags: Preference<Int> by lazy {
        preferenceStore.getInt("migrate_flags", Int.MAX_VALUE)
    }

    private val enhancedServices by lazy { Injekt.get<TrackManager>().services.filterIsInstance<EnhancedAnimeTrackService>() }

    suspend fun migrateAnime(oldAnime: Anime, newAnime: Anime, replace: Boolean) {
        val source = sourceManager.get(newAnime.source) ?: return
        val prevAnimeSource = sourceManager.get(oldAnime.source)

        try {
            val episodes = source.getEpisodeList(newAnime.toSAnime())

            migrateAnimeInternal(
                oldAnimeSource = prevAnimeSource,
                newAnimeSource = source,
                oldAnime = oldAnime,
                newAnime = newAnime,
                sourceEpisodes = episodes,
                replace = replace,
            )
        } catch (_: Throwable) { }
    }

    private suspend fun migrateAnimeInternal(
        oldAnimeSource: AnimeSource?,
        newAnimeSource: AnimeSource,
        oldAnime: Anime,
        newAnime: Anime,
        sourceEpisodes: List<SEpisode>,
        replace: Boolean,
    ) {
        val flags = migrateFlags.get()

        val migrateEpisodes = AnimeMigrationFlags.hasEpisodes(flags)
        val migrateCategories = AnimeMigrationFlags.hasCategories(flags)
        val migrateTracks = AnimeMigrationFlags.hasTracks(flags)
        val migrateCustomCover = AnimeMigrationFlags.hasCustomCover(flags)

        try {
            syncEpisodesWithAnimeSource.await(sourceEpisodes, newAnime, newAnimeSource)
        } catch (e: Exception) {
            // Worst case, episodes won't be synced
        }

        // Update episodes seen, bookmark and dateFetch
        if (migrateEpisodes) {
            val prevAnimeEpisodes = getEpisodeByAnimeId.await(oldAnime.id)
            val animeEpisodes = getEpisodeByAnimeId.await(newAnime.id)

            val maxEpisodeSeen = prevAnimeEpisodes
                .filter { it.seen }
                .maxOfOrNull { it.episodeNumber }

            val updatedAnimeEpisodes = animeEpisodes.map { animeEpisode ->
                var updatedEpisode = animeEpisode
                if (updatedEpisode.isRecognizedNumber) {
                    val prevEpisode = prevAnimeEpisodes
                        .find { it.isRecognizedNumber && it.episodeNumber == updatedEpisode.episodeNumber }

                    if (prevEpisode != null) {
                        updatedEpisode = updatedEpisode.copy(
                            dateFetch = prevEpisode.dateFetch,
                            bookmark = prevEpisode.bookmark,
                        )
                    }

                    if (maxEpisodeSeen != null && updatedEpisode.episodeNumber <= maxEpisodeSeen) {
                        updatedEpisode = updatedEpisode.copy(seen = true)
                    }
                }

                updatedEpisode
            }

            val episodeUpdates = updatedAnimeEpisodes.map { it.toEpisodeUpdate() }
            updateEpisode.awaitAll(episodeUpdates)
        }

        // Update categories
        if (migrateCategories) {
            val categoryIds = getCategories.await(oldAnime.id).map { it.id }
            setAnimeCategories.await(newAnime.id, categoryIds)
        }

        // Update track
        if (migrateTracks) {
            val tracks = getTracks.await(oldAnime.id).mapNotNull { track ->
                val updatedTrack = track.copy(animeId = newAnime.id)

                val service = enhancedServices
                    .firstOrNull { it.isTrackFrom(updatedTrack, oldAnime, oldAnimeSource) }

                if (service != null) {
                    service.migrateTrack(updatedTrack, newAnime, newAnimeSource)
                } else {
                    updatedTrack
                }
            }
            insertTrack.awaitAll(tracks)
        }

        if (replace) {
            updateAnime.await(AnimeUpdate(oldAnime.id, favorite = false, dateAdded = 0))
        }

        // Update custom cover (recheck if custom cover exists)
        if (migrateCustomCover && oldAnime.hasCustomCover()) {
            @Suppress("BlockingMethodInNonBlockingContext")
            coverCache.setCustomCoverToCache(newAnime, coverCache.getCustomCoverFile(oldAnime.id).inputStream())
        }

        updateAnime.await(
            AnimeUpdate(
                id = newAnime.id,
                favorite = true,
                episodeFlags = oldAnime.episodeFlags,
                viewerFlags = oldAnime.viewerFlags,
                dateAdded = if (replace) oldAnime.dateAdded else Date().time,
            ),
        )
    }
}
