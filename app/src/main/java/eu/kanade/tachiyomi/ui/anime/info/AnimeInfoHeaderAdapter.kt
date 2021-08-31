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
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.MangaInfoHeaderBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.getMainAppBarHeight
import eu.kanade.tachiyomi.util.system.applySystemAnimatorScale
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.view.loadAnyAutoPause
import eu.kanade.tachiyomi.util.view.setChips
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.view.longClicks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class AnimeInfoHeaderAdapter(
    private val controller: AnimeController,
    private val fromSource: Boolean,
    private val isTablet: Boolean,
) :
    RecyclerView.Adapter<AnimeInfoHeaderAdapter.HeaderViewHolder>() {

    private val trackManager: TrackManager by injectLazy()

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
            val summaryTransition = binding.mangaSummarySection.getTransition(R.id.manga_summary_section_transition)
            summaryTransition.applySystemAnimatorScale(view.context)

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
                    text = animeSource
                    setOnClickListener {
                        val sourceManager = Injekt.get<AnimeSourceManager>()
                        controller.performSearch(sourceManager.getOrStub(source.id).name)
                    }
                } else {
                    text = view.context.getString(R.string.unknown)
                }
            }

            // Update status TextView.
            binding.mangaStatus.setText(
                when (anime.status) {
                    SAnime.ONGOING -> R.string.ongoing
                    SAnime.COMPLETED -> R.string.completed
                    SAnime.LICENSED -> R.string.licensed
                    else -> R.string.unknown_status
                }
            )

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
                binding.mangaSummaryText.text = if (anime.description.isNullOrBlank()) {
                    view.context.getString(R.string.unknown)
                } else {
                    // Max lines of 3 with a blank line looks whack so we remove
                    // any line breaks that is 2 or more and replace it with 1
                    anime.description!!
                        .replace(Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE)), "\n")
                }

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
                    binding.mangaGenresTagsCompactChips.isVisible = false
                    binding.mangaGenresTagsFullChips.isVisible = false
                }

                // Handle showing more or less info
                merge(
                    binding.mangaSummaryText.clicks(),
                    binding.mangaInfoToggleMore.clicks(),
                    binding.mangaInfoToggleLess.clicks(),
                    binding.mangaSummarySection.clicks()
                )
                    .onEach { toggleAnimeInfo() }
                    .launchIn(controller.viewScope)

                // Expand anime info if navigated from source listing or explicitly set to
                // (e.g. on tablets)
                if (initialLoad && (fromSource || isTablet)) {
                    toggleAnimeInfo()
                    initialLoad = false
                    // wrap_content and autoFixTextSize can cause unwanted behaviour this tries to solve it
                    binding.mangaFullTitle.requestLayout()
                }

                // Refreshes will change the state and it needs to be set to correct state to display correctly
                if (binding.mangaSummaryText.maxLines == maxLines) {
                    binding.mangaSummarySection.transitionToState(R.id.start)
                } else {
                    binding.mangaSummarySection.transitionToState(R.id.end)
                }
            }
        }

        private fun showAnimeInfo(visible: Boolean) {
            binding.mangaSummarySection.isVisible = visible
        }

        private fun toggleAnimeInfo() {
            val isCurrentlyExpanded = binding.mangaSummaryText.maxLines != maxLines

            if (isCurrentlyExpanded) {
                binding.mangaSummarySection.transitionToStart()
            } else {
                binding.mangaSummarySection.transitionToEnd()
            }

            binding.mangaSummaryText.maxLines = if (isCurrentlyExpanded) {
                maxLines
            } else {
                Int.MAX_VALUE
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
            binding.btnFavorite.apply {
                setIconResource(if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_border_24dp)
                text =
                    context.getString(if (isFavorite) R.string.in_library else R.string.add_to_library)
                isActivated = isFavorite
            }
        }
    }
}
