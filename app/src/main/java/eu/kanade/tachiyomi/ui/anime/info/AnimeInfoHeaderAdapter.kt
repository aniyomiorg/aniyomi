package eu.kanade.tachiyomi.ui.anime.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.MangaInfoHeaderBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.getMainAppBarHeight
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.view.loadAnyAutoPause
import eu.kanade.tachiyomi.util.view.setChips
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.view.longClicks
import uy.kohesive.injekt.injectLazy

class AnimeInfoHeaderAdapter(
    private val controller: AnimeController,
    private val fromSource: Boolean,
    private val isTablet: Boolean,
) :
    RecyclerView.Adapter<AnimeInfoHeaderAdapter.HeaderViewHolder>() {

    private val trackManager: TrackManager by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()
    private val sourceManager: AnimeSourceManager by injectLazy()

    private var anime: Anime = controller.presenter.anime
    private var source: AnimeSource = controller.presenter.source
    private var trackCount: Int = 0

    private lateinit var binding: MangaInfoHeaderBinding

    private var initialLoad: Boolean = true

    private val maxLines = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = MangaInfoHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        updateCoverPosition()
        return HeaderViewHolder(binding.root)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    /**
     * Update the view with anime information.
     *
     * @param anime anime object containing information about anime.
     * @param source the source of the anime.
     */
    fun update(anime: Anime, source: AnimeSource) {
        this.anime = anime
        this.source = source

        notifyDataSetChanged()
    }

    fun setTrackingCount(trackCount: Int) {
        this.trackCount = trackCount

        notifyDataSetChanged()
    }

    private fun updateCoverPosition() {
        val appBarHeight = controller.getMainAppBarHeight()
        binding.mangaCover.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin += appBarHeight
        }
    }

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            // For rounded corners
            binding.mangaCover.clipToOutline = true

            binding.btnFavorite.clicks()
                .onEach { controller.onFavoriteClick() }
                .launchIn(controller.viewScope)

            if (controller.presenter.anime.favorite && controller.presenter.getCategories().isNotEmpty()) {
                binding.btnFavorite.longClicks()
                    .onEach { controller.onCategoriesClick() }
                    .launchIn(controller.viewScope)
            }

            with(binding.btnTracking) {
                if (trackManager.hasLoggedServices()) {
                    isVisible = true

                    if (trackCount > 0) {
                        setIconResource(R.drawable.ic_done_24dp)
                        text = view.context.resources.getQuantityString(
                            R.plurals.num_trackers,
                            trackCount,
                            trackCount
                        )
                        isActivated = true
                    } else {
                        setIconResource(R.drawable.ic_sync_24dp)
                        text = view.context.getString(R.string.manga_tracking_tab)
                        isActivated = false
                    }

                    clicks()
                        .onEach { controller.onTrackingClick() }
                        .launchIn(controller.viewScope)
                } else {
                    isVisible = false
                }
            }

            if (controller.presenter.source is AnimeHttpSource) {
                binding.btnWebview.isVisible = true
                binding.btnWebview.clicks()
                    .onEach { controller.openAnimeInWebView() }
                    .launchIn(controller.viewScope)
            }

            binding.mangaFullTitle.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        binding.mangaFullTitle.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaFullTitle.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaFullTitle.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaAuthor.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaAuthor.text.toString(),
                        binding.mangaAuthor.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaAuthor.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaAuthor.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaArtist.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaArtist.text.toString(),
                        binding.mangaArtist.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaArtist.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaArtist.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaSummaryText.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.description),
                        binding.mangaSummaryText.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaCover.clicks()
                .onEach {
                    controller.showFullCoverDialog()
                }
                .launchIn(controller.viewScope)

            binding.mangaCover.longClicks()
                .onEach {
                    showCoverOptionsDialog()
                }
                .launchIn(controller.viewScope)

            setAnimeInfo(anime, source)
        }

        private fun showCoverOptionsDialog() {
            val options = listOfNotNull(
                R.string.action_share,
                R.string.action_save,
                // Can only edit cover for library anime
                if (anime.favorite) R.string.action_edit else null
            ).map(controller.activity!!::getString).toTypedArray()

            MaterialAlertDialogBuilder(controller.activity!!)
                .setTitle(R.string.manga_cover)
                .setItems(options) { _, item ->
                    when (item) {
                        0 -> controller.shareCover()
                        1 -> controller.saveCover()
                        2 -> controller.changeCover()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        /**
         * Update the view with anime information.
         *
         * @param anime anime object containing information about anime.
         * @param source the source of the anime.
         */
        private fun setAnimeInfo(anime: Anime, source: AnimeSource?) {
            // Update full title TextView.
            binding.mangaFullTitle.text = if (anime.title.isBlank()) {
                view.context.getString(R.string.unknown)
            } else {
                anime.title
            }

            // Update author TextView.
            binding.mangaAuthor.text = if (anime.author.isNullOrBlank()) {
                view.context.getString(R.string.unknown_author)
            } else {
                anime.author
            }

            // Update artist TextView.
            val hasArtist = !anime.artist.isNullOrBlank() && anime.artist != anime.author
            binding.mangaArtist.isVisible = hasArtist
            if (hasArtist) {
                binding.mangaArtist.text = anime.artist
            }

            // If anime source is known update source TextView.
            val animeSource = source?.toString()
            with(binding.mangaSource) {
                if (animeSource != null) {
                    val enabledLanguages = preferences.enabledLanguages().get()
                        .filterNot { it in listOf("all", "other") }

                    val hasOneActiveLanguages = enabledLanguages.size == 1
                    val isInEnabledLanguages = source.lang in enabledLanguages
                    text = when {
                        // For edge cases where user disables a source they got manga of in their library.
                        hasOneActiveLanguages && !isInEnabledLanguages -> animeSource
                        // Hide the language tag when only one language is used.
                        hasOneActiveLanguages && isInEnabledLanguages -> source.name
                        else -> animeSource
                    }
                    setOnClickListener {
                        controller.performSearch(sourceManager.getOrStub(source.id).name)
                    }
                } else {
                    text = view.context.getString(R.string.unknown)
                }
            }

            // Update anime status.
            val (statusDrawable, statusString) = when (anime.status) {
                SAnime.ONGOING -> R.drawable.ic_status_ongoing_24dp to R.string.ongoing
                SAnime.COMPLETED -> R.drawable.ic_status_completed_24dp to R.string.completed
                SAnime.LICENSED -> R.drawable.ic_status_licensed_24dp to R.string.licensed
                else -> R.drawable.ic_status_unknown_24dp to R.string.unknown
            }
            binding.mangaStatusIcon.setImageResource(statusDrawable)
            binding.mangaStatus.setText(statusString)

            // Set the favorite drawable to the correct one.
            setFavoriteButtonState(anime.favorite)

            // Set cover if changed.
            binding.backdrop.loadAnyAutoPause(anime)
            binding.mangaCover.loadAnyAutoPause(anime)

            // Anime info section
            val hasInfoContent = !anime.description.isNullOrBlank() || !anime.genre.isNullOrBlank()
            showAnimeInfo(hasInfoContent)
            if (hasInfoContent) {
                // Update description TextView.
                binding.mangaSummaryText.text = updateDescription(anime.description, (fromSource || isTablet).not())

                // Update genres list
                if (!anime.genre.isNullOrBlank()) {
                    binding.mangaGenresTagsCompactChips.setChips(
                        anime.getGenres(),
                        controller::performGenreSearch
                    )
                    binding.mangaGenresTagsFullChips.setChips(
                        anime.getGenres(),
                        controller::performGenreSearch
                    )
                } else {
                    binding.mangaGenresTagsCompact.isVisible = false
                    binding.mangaGenresTagsCompactChips.isVisible = false
                    binding.mangaGenresTagsFullChips.isVisible = false
                }

                // Handle showing more or less info
                merge(
                    binding.mangaSummaryText.clicks(),
                    binding.mangaInfoToggleMore.clicks(),
                    binding.mangaInfoToggleLess.clicks(),
                    binding.mangaSummarySection.clicks(),
                )
                    .onEach { toggleAnimeInfo() }
                    .launchIn(controller.viewScope)

                if (initialLoad) {
                    binding.mangaGenresTagsCompact.requestLayout()
                }

                // Expand anime info if navigated from source listing or explicitly set to
                // (e.g. on tablets)
                if (initialLoad && (fromSource || isTablet)) {
                    toggleAnimeInfo()
                    initialLoad = false
                }
            }
        }

        private fun showAnimeInfo(visible: Boolean) {
            binding.mangaSummarySection.isVisible = visible
        }

        private fun toggleAnimeInfo() {
            val isCurrentlyExpanded = binding.mangaSummaryText.maxLines != maxLines

            binding.mangaInfoToggleMore.isVisible = isCurrentlyExpanded
            binding.mangaInfoScrim.isVisible = isCurrentlyExpanded
            binding.mangaInfoToggleMoreScrim.isVisible = isCurrentlyExpanded
            binding.mangaGenresTagsCompact.isVisible = isCurrentlyExpanded
            binding.mangaGenresTagsCompactChips.isVisible = isCurrentlyExpanded

            binding.mangaInfoToggleLess.isVisible = !isCurrentlyExpanded
            binding.mangaGenresTagsFullChips.isVisible = !isCurrentlyExpanded

            binding.mangaSummaryText.text =
                updateDescription(anime.description, isCurrentlyExpanded)

            binding.mangaSummaryText.maxLines = when {
                isCurrentlyExpanded -> maxLines
                else -> Int.MAX_VALUE
            }
        }

        private fun updateDescription(description: String?, isCurrentlyExpanded: Boolean): CharSequence? {
            return when {
                description.isNullOrBlank() -> view.context.getString(R.string.unknown)
                isCurrentlyExpanded -> description.replace(Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE)), "\n")
                else -> description
            }
        }

        /**
         * Update favorite button with correct drawable and text.
         *
         * @param isFavorite determines if anime is favorite or not.
         */
        private fun setFavoriteButtonState(isFavorite: Boolean) {
            // Set the Favorite drawable to the correct one.
            // Border drawable if false, filled drawable if true.
            val (iconResource, stringResource) = when (isFavorite) {
                true -> R.drawable.ic_favorite_24dp to R.string.in_library
                false -> R.drawable.ic_favorite_border_24dp to R.string.add_to_library
            }
            binding.btnFavorite.apply {
                setIconResource(iconResource)
                text = context.getString(stringResource)
                isActivated = isFavorite
            }
        }
    }
}
