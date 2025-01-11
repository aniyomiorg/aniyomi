package eu.kanade.tachiyomi.ui.entries.anime

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import aniyomi.ui.metadata.adapters.MetadataUIUtil.getResourceColor
import aniyomi.util.dropBlank
import aniyomi.util.trimOrNull
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.track.EnhancedAnimeTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.databinding.EditAnimeDialogBinding
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.materialdialogs.setTextInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.i18n.MR
import tachiyomi.source.local.entries.anime.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
@Suppress("MagicNumber", "LongMethod")
fun EditAnimeDialog(
    anime: Anime,
    onDismissRequest: () -> Unit,
    onPositiveClick: (
        title: String?,
        author: String?,
        artist: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var binding by remember {
        mutableStateOf<EditAnimeDialogBinding?>(null)
    }
    val showTrackerSelectionDialogue = remember { mutableStateOf(false) }
    val getTracks = remember { Injekt.get<GetAnimeTracks>() }
    val trackerManager = remember { Injekt.get<TrackerManager>() }
    val tracks = remember { mutableStateOf(emptyList<Pair<AnimeTrack, Tracker>>()) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val binding = binding ?: return@TextButton
                    onPositiveClick(
                        binding.title.text.toString(),
                        binding.animeAuthor.text.toString(),
                        binding.animeArtist.text.toString(),
                        binding.animeDescription.text.toString(),
                        binding.animeGenresTags.getTextStrings(),
                        binding.status.selectedItemPosition.let {
                            when (it) {
                                1 -> SAnime.ONGOING
                                2 -> SAnime.COMPLETED
                                3 -> SAnime.LICENSED
                                4 -> SAnime.PUBLISHING_FINISHED
                                5 -> SAnime.CANCELLED
                                6 -> SAnime.ON_HIATUS
                                else -> null
                            }
                        }?.toLong(),
                    )
                    onDismissRequest()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                AndroidView(
                    factory = { factoryContext ->
                        EditAnimeDialogBinding.inflate(LayoutInflater.from(factoryContext))
                            .also { binding = it }
                            .apply {
                                onViewCreated(
                                    anime,
                                    factoryContext,
                                    this,
                                    scope,
                                    getTracks,
                                    trackerManager,
                                    tracks,
                                    showTrackerSelectionDialogue,
                                )
                            }
                            .root
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
    if (showTrackerSelectionDialogue.value) {
        TrackerSelectDialog(
            tracks = tracks.value,
            onDismissRequest = { showTrackerSelectionDialogue.value = false },
            onTrackerSelect = { tracker, track ->
                scope.launch {
                    autofillFromTracker(binding!!, track, tracker)
                }
            },
        )
    }
}

@Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun TrackerSelectDialog(
    tracks: List<Pair<AnimeTrack, Tracker>>,
    onDismissRequest: () -> Unit,
    onTrackerSelect: (
        tracker: Tracker,
        track: AnimeTrack,
    ) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = {
            Text(stringResource(R.string.select_tracker))
        },
        text = {
            FlowRow(
                modifier = Modifier
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tracks.forEach { (AnimeTrack, tracker) ->
                    TrackLogoIcon(
                        tracker,
                        onClick = {
                            onTrackerSelect(tracker, AnimeTrack)
                            onDismissRequest()
                        },
                    )
                }
            }
        },
    )
}

private fun onViewCreated(
    anime: Anime,
    context: Context,
    binding: EditAnimeDialogBinding,
    scope: CoroutineScope,
    getTracks: GetAnimeTracks,
    trackerManager: TrackerManager,
    tracks: MutableState<List<Pair<AnimeTrack, Tracker>>>,
    showTrackerSelectionDialogue: MutableState<Boolean>,
) {
    loadCover(anime, binding)

    val statusAdapter: ArrayAdapter<String> = ArrayAdapter(
        context,
        android.R.layout.simple_spinner_dropdown_item,
        listOf(
            R.string.label_default,
            R.string.ongoing,
            R.string.completed,
            R.string.licensed,
            R.string.publishing_finished,
            R.string.cancelled,
            R.string.on_hiatus,
        ).map { context.getString(it) },
    )

    binding.status.adapter = statusAdapter
    if (anime.status != anime.ogStatus) {
        binding.status.setSelection(
            when (anime.status.toInt()) {
                SAnime.UNKNOWN -> 0
                SAnime.ONGOING -> 1
                SAnime.COMPLETED -> 2
                SAnime.LICENSED -> 3
                SAnime.PUBLISHING_FINISHED, 61 -> 4
                SAnime.CANCELLED, 62 -> 5
                SAnime.ON_HIATUS, 63 -> 6
                else -> 0
            },
        )
    }

    if (anime.isLocal()) {
        if (anime.title != anime.url) {
            binding.title.setText(anime.title)
        }

        binding.title.hint = context.getString(R.string.title_hint, anime.url)
        binding.animeAuthor.setText(anime.author.orEmpty())
        binding.animeArtist.setText(anime.artist.orEmpty())
        binding.animeDescription.setText(anime.description.orEmpty())
        binding.animeGenresTags.setChips(anime.genre.orEmpty().dropBlank(), scope)
    } else {
        if (anime.title != anime.ogTitle) {
            binding.title.append(anime.title)
        }
        if (anime.author != anime.ogAuthor) {
            binding.animeAuthor.append(anime.author.orEmpty())
        }
        if (anime.artist != anime.ogArtist) {
            binding.animeArtist.append(anime.artist.orEmpty())
        }
        if (anime.description != anime.ogDescription) {
            binding.animeDescription.append(anime.description.orEmpty())
        }
        binding.animeGenresTags.setChips(anime.genre.orEmpty().dropBlank(), scope)

        binding.title.hint = context.getString(R.string.title_hint, anime.ogTitle)
        binding.animeAuthor.hint = context.getString(R.string.author_hint, anime.ogAuthor ?: "")
        binding.animeArtist.hint = context.getString(R.string.artist_hint, anime.ogArtist ?: "")
        binding.animeDescription.hint =
            context.getString(
                R.string.description_hint,
                anime.ogDescription?.takeIf { it.isNotBlank() }?.let { it.replace("\n", " ").chop(20) } ?: "",
            )
    }
    binding.animeGenresTags.clearFocus()

    binding.resetTags.setOnClickListener { resetTags(anime, binding, scope) }
    // SY -->
    binding.resetInfo.setOnClickListener { resetInfo(anime, binding, scope) }
    binding.autofillFromTracker.setOnClickListener {
        scope.launch {
            getTrackers(
                anime,
                binding,
                context,
                getTracks,
                trackerManager,
                tracks,
                showTrackerSelectionDialogue,
            )
        }
    }
}

private suspend fun getTrackers(
    anime: Anime,
    binding: EditAnimeDialogBinding,
    context: Context,
    getTracks: GetAnimeTracks,
    trackerManager: TrackerManager,
    tracks: MutableState<List<Pair<AnimeTrack, Tracker>>>,
    showTrackerSelectionDialogue: MutableState<Boolean>,
) {
    tracks.value = getTracks.await(anime.id).map { track ->
        track to trackerManager.get(track.trackerId)!!
    }
        .filterNot { (_, tracker) -> tracker is EnhancedAnimeTracker }

    if (tracks.value.isEmpty()) {
        context.toast(context.stringResource(MR.strings.entry_not_tracked))
        return
    }

    if (tracks.value.size > 1) {
        showTrackerSelectionDialogue.value = true
        return
    }

    autofillFromTracker(binding, tracks.value.first().first, tracks.value.first().second)
}

private fun setTextIfNotBlank(field: (String) -> Unit, value: String?) {
    value?.takeIf { it.isNotBlank() }?.let { field(it) }
}

private suspend fun autofillFromTracker(binding: EditAnimeDialogBinding, track: AnimeTrack, tracker: Tracker) {
    try {
        val trackerAnimeMetadata = tracker.getAnimeMetadata(track)

        setTextIfNotBlank(binding.title::setText, trackerAnimeMetadata?.title)
        setTextIfNotBlank(binding.animeAuthor::setText, trackerAnimeMetadata?.authors)
        setTextIfNotBlank(binding.animeArtist::setText, trackerAnimeMetadata?.artists)
        setTextIfNotBlank(binding.animeDescription::setText, trackerAnimeMetadata?.description)
    } catch (e: Throwable) {
        tracker.logcat(LogPriority.ERROR, e)
        binding.root.context.toast(
            binding.root.context.stringResource(
                MR.strings.track_error,
                tracker.name,
                e.message ?: "",
            ),
        )
    }
    // SY<--
}

private fun resetTags(anime: Anime, binding: EditAnimeDialogBinding, scope: CoroutineScope) {
    if (anime.genre.isNullOrEmpty() || anime.isLocal()) {
        binding.animeGenresTags.setChips(emptyList(), scope)
    } else {
        binding.animeGenresTags.setChips(anime.ogGenre.orEmpty(), scope)
    }
}

private fun resetInfo(anime: Anime, binding: EditAnimeDialogBinding, scope: CoroutineScope) {
    binding.title.setText("")
    binding.animeAuthor.setText("")
    binding.animeArtist.setText("")
    binding.animeDescription.setText("")
    resetTags(anime, binding, scope)
}

private fun loadCover(anime: Anime, binding: EditAnimeDialogBinding) {
    binding.animeCover.load(anime) {
        transformations(RoundedCornersTransformation(4.dpToPx.toFloat()))
    }
}

private fun ChipGroup.setChips(items: List<String>, scope: CoroutineScope) {
    removeAllViews()

    items.asSequence().map { item ->
        Chip(context).apply {
            text = item

            isCloseIconVisible = true
            closeIcon?.setTint(context.getResourceColor(R.attr.colorAccent))
            setOnCloseIconClickListener {
                removeView(this)
            }
        }
    }.forEach {
        addView(it)
    }

    val addTagChip = Chip(context).apply {
        setText(R.string.add_tag)

        chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_add_24dp)?.apply {
            isChipIconVisible = true
            setTint(context.getResourceColor(R.attr.colorAccent))
        }

        setOnClickListener {
            var newTag: String? = null
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.add_tag)
                .setTextInput {
                    newTag = it.trimOrNull()
                }
                .setPositiveButton(R.string.action_ok) { _, _ ->
                    if (newTag != null) setChips(items + listOfNotNull(newTag), scope)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
    addView(addTagChip)
}

private fun ChipGroup.getTextStrings(): List<String> = children.mapNotNull {
    if (it is Chip &&
        !it.text.toString().contains(
            context.getString(R.string.add_tag),
            ignoreCase = true,
        )
    ) {
        it.text.toString()
    } else {
        null
    }
}.toList()
