package eu.kanade.tachiyomi.ui.entries.manga

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
import eu.kanade.tachiyomi.data.track.EnhancedMangaTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.materialdialogs.setTextInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import tachiyomi.domain.track.manga.model.MangaTrack
import tachiyomi.i18n.MR
import tachiyomi.source.local.entries.manga.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
@Suppress("MagicNumber", "LongMethod")
fun EditMangaDialog(
    manga: Manga,
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
        mutableStateOf<EditMangaDialogBinding?>(null)
    }
    val showTrackerSelectionDialogue = remember { mutableStateOf(false) }
    val getTracks = remember { Injekt.get<GetMangaTracks>() }
    val trackerManager = remember { Injekt.get<TrackerManager>() }
    val tracks = remember { mutableStateOf(emptyList<Pair<MangaTrack, Tracker>>()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val binding = binding ?: return@TextButton
                    onPositiveClick(
                        binding.title.text.toString(),
                        binding.mangaAuthor.text.toString(),
                        binding.mangaArtist.text.toString(),
                        binding.mangaDescription.text.toString(),
                        binding.mangaGenresTags.getTextStrings(),
                        binding.status.selectedItemPosition.let {
                            when (it) {
                                1 -> SManga.ONGOING
                                2 -> SManga.COMPLETED
                                3 -> SManga.LICENSED
                                4 -> SManga.PUBLISHING_FINISHED
                                5 -> SManga.CANCELLED
                                6 -> SManga.ON_HIATUS
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
                        EditMangaDialogBinding.inflate(LayoutInflater.from(factoryContext))
                            .also { binding = it }
                            .apply {
                                onViewCreated(
                                    manga,
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
    tracks: List<Pair<MangaTrack, Tracker>>,
    onDismissRequest: () -> Unit,
    onTrackerSelect: (
        tracker: Tracker,
        track: MangaTrack,
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
                tracks.forEach { (MangaTrack, tracker) ->
                    TrackLogoIcon(
                        tracker,
                        onClick = {
                            onTrackerSelect(tracker, MangaTrack)
                            onDismissRequest()
                        },
                    )
                }
            }
        },
    )
}

private fun onViewCreated(
    manga: Manga,
    context: Context,
    binding: EditMangaDialogBinding,
    scope: CoroutineScope,
    getTracks: GetMangaTracks,
    trackerManager: TrackerManager,
    tracks: MutableState<List<Pair<MangaTrack, Tracker>>>,
    showTrackerSelectionDialogue: MutableState<Boolean>,
) {
    loadCover(manga, binding)

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
    if (manga.status != manga.ogStatus) {
        binding.status.setSelection(
            when (manga.status.toInt()) {
                SManga.UNKNOWN -> 0
                SManga.ONGOING -> 1
                SManga.COMPLETED -> 2
                SManga.LICENSED -> 3
                SManga.PUBLISHING_FINISHED, 61 -> 4
                SManga.CANCELLED, 62 -> 5
                SManga.ON_HIATUS, 63 -> 6
                else -> 0
            },
        )
    }

    if (manga.isLocal()) {
        if (manga.title != manga.url) {
            binding.title.setText(manga.title)
        }

        binding.title.hint = context.getString(R.string.title_hint, manga.url)
        binding.mangaAuthor.setText(manga.author.orEmpty())
        binding.mangaArtist.setText(manga.artist.orEmpty())
        binding.mangaDescription.setText(manga.description.orEmpty())
        binding.mangaGenresTags.setChips(manga.genre.orEmpty().dropBlank(), scope)
    } else {
        if (manga.title != manga.ogTitle) {
            binding.title.append(manga.title)
        }
        if (manga.author != manga.ogAuthor) {
            binding.mangaAuthor.append(manga.author.orEmpty())
        }
        if (manga.artist != manga.ogArtist) {
            binding.mangaArtist.append(manga.artist.orEmpty())
        }
        if (manga.description != manga.ogDescription) {
            binding.mangaDescription.append(manga.description.orEmpty())
        }
        binding.mangaGenresTags.setChips(manga.genre.orEmpty().dropBlank(), scope)

        binding.title.hint = context.getString(R.string.title_hint, manga.ogTitle)
        binding.mangaAuthor.hint = context.getString(R.string.author_hint, manga.ogAuthor ?: "")
        binding.mangaArtist.hint = context.getString(R.string.artist_hint, manga.ogArtist ?: "")
        binding.mangaDescription.hint =
            context.getString(
                R.string.description_hint,
                manga.ogDescription?.takeIf { it.isNotBlank() }?.let { it.replace("\n", " ").chop(20) } ?: "",
            )
    }
    binding.mangaGenresTags.clearFocus()

    binding.resetTags.setOnClickListener { resetTags(manga, binding, scope) }
    // SY-->
    binding.resetInfo.setOnClickListener { resetInfo(manga, binding, scope) }
    binding.autofillFromTracker.setOnClickListener {
        scope.launch {
            getTrackers(
                manga,
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
    manga: Manga,
    binding: EditMangaDialogBinding,
    context: Context,
    getTracks: GetMangaTracks,
    trackerManager: TrackerManager,
    tracks: MutableState<List<Pair<MangaTrack, Tracker>>>,
    showTrackerSelectionDialogue: MutableState<Boolean>,
) {
    tracks.value = getTracks.await(manga.id).map { track ->
        track to trackerManager.get(track.trackerId)!!
    }
        .filterNot { (_, tracker) -> tracker is EnhancedMangaTracker }

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

private suspend fun autofillFromTracker(binding: EditMangaDialogBinding, track: MangaTrack, tracker: Tracker) {
    try {
        val trackerMangaMetadata = tracker.getMangaMetadata(track)

        setTextIfNotBlank(binding.title::setText, trackerMangaMetadata?.title)
        setTextIfNotBlank(binding.mangaAuthor::setText, trackerMangaMetadata?.authors)
        setTextIfNotBlank(binding.mangaArtist::setText, trackerMangaMetadata?.artists)
        setTextIfNotBlank(binding.mangaDescription::setText, trackerMangaMetadata?.description)
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

private fun resetTags(manga: Manga, binding: EditMangaDialogBinding, scope: CoroutineScope) {
    if (manga.genre.isNullOrEmpty() || manga.isLocal()) {
        binding.mangaGenresTags.setChips(emptyList(), scope)
    } else {
        binding.mangaGenresTags.setChips(manga.ogGenre.orEmpty(), scope)
    }
}
private fun resetInfo(manga: Manga, binding: EditMangaDialogBinding, scope: CoroutineScope) {
    binding.title.setText("")
    binding.mangaAuthor.setText("")
    binding.mangaArtist.setText("")
    binding.mangaDescription.setText("")
    resetTags(manga, binding, scope)
}

private fun loadCover(manga: Manga, binding: EditMangaDialogBinding) {
    binding.mangaCover.load(manga) {
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
