package eu.kanade.tachiyomi.ui.anime.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.loadAny
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.AnimeInfoHeaderBinding
import eu.kanade.tachiyomi.source.AnimeSource
import eu.kanade.tachiyomi.source.AnimeSourceManager
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.util.system.copyToClipboard
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
    private val fromSource: Boolean
) :
    RecyclerView.Adapter<AnimeInfoHeaderAdapter.HeaderViewHolder>() {

    private val trackManager: TrackManager by injectLazy()

    private var anime: Anime = controller.presenter.anime
    private var source: AnimeSource = controller.presenter.source
    private var trackCount: Int = 0

    private lateinit var binding: AnimeInfoHeaderBinding

    private var initialLoad: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = AnimeInfoHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            // For rounded corners
            binding.animeCover.clipToOutline = true

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

            if (controller.presenter.source is HttpSource) {
                binding.btnWebview.isVisible = true
                binding.btnWebview.clicks()
                    .onEach { controller.openAnimeInWebView() }
                    .launchIn(controller.viewScope)
            }

            binding.animeFullTitle.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        binding.animeFullTitle.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.animeFullTitle.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.animeFullTitle.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.animeAuthor.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.animeAuthor.text.toString(),
                        binding.animeAuthor.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.animeAuthor.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.animeAuthor.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.animeArtist.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.animeArtist.text.toString(),
                        binding.animeArtist.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.animeArtist.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.animeArtist.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.animeSummaryText.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.description),
                        binding.animeSummaryText.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.animeCover.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        controller.presenter.anime.title
                    )
                }
                .launchIn(controller.viewScope)

            setAnimeInfo(anime, source)
        }

        /**
         * Update the view with anime information.
         *
         * @param anime anime object containing information about anime.
         * @param source the source of the anime.
         */
        private fun setAnimeInfo(anime: Anime, source: AnimeSource?) {
            // Update full title TextView.
            binding.animeFullTitle.text = if (anime.title.isBlank()) {
                view.context.getString(R.string.unknown)
            } else {
                anime.title
            }

            // Update author TextView.
            binding.animeAuthor.text = if (anime.author.isNullOrBlank()) {
                view.context.getString(R.string.unknown_author)
            } else {
                anime.author
            }

            // Update artist TextView.
            val hasArtist = !anime.artist.isNullOrBlank() && anime.artist != anime.author
            binding.animeArtist.isVisible = hasArtist
            if (hasArtist) {
                binding.animeArtist.text = anime.artist
            }

            // If anime source is known update source TextView.
            val animeSource = source?.toString()
            with(binding.animeSource) {
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
            binding.animeStatus.setText(
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
            listOf(binding.animeCover, binding.backdrop).forEach {
                it.loadAny(anime.thumbnail_url)
            }

            // Anime info section
            val hasInfoContent = !anime.description.isNullOrBlank() || !anime.genre.isNullOrBlank()
            showAnimeInfo(hasInfoContent)
            if (hasInfoContent) {
                // Update description TextView.
                binding.animeSummaryText.text = if (anime.description.isNullOrBlank()) {
                    view.context.getString(R.string.unknown)
                } else {
                    anime.description
                }

                // Update genres list
                if (!anime.genre.isNullOrBlank()) {
                    binding.animeGenresTagsCompactChips.setChips(
                        anime.getGenres(),
                        controller::performSearch
                    )
                    binding.animeGenresTagsFullChips.setChips(
                        anime.getGenres(),
                        controller::performSearch
                    )
                } else {
                    binding.animeGenresTagsCompactChips.isVisible = false
                    binding.animeGenresTagsFullChips.isVisible = false
                }

                // Handle showing more or less info
                merge(
                    binding.animeSummarySection.clicks(),
                    binding.animeSummaryText.clicks(),
                    binding.animeInfoToggleMore.clicks(),
                    binding.animeInfoToggleLess.clicks()
                )
                    .onEach { toggleAnimeInfo() }
                    .launchIn(controller.viewScope)

                // Expand anime info if navigated from source listing
                if (initialLoad && fromSource) {
                    toggleAnimeInfo()
                    initialLoad = false
                }
            }
        }

        private fun showAnimeInfo(visible: Boolean) {
            binding.animeSummarySection.isVisible = visible
        }

        private fun toggleAnimeInfo() {
            val isCurrentlyExpanded = binding.animeSummaryText.maxLines != 2

            binding.animeInfoToggleMoreScrim.isVisible = isCurrentlyExpanded
            binding.animeInfoToggleMore.isVisible = isCurrentlyExpanded
            binding.animeInfoToggleLess.isVisible = !isCurrentlyExpanded

            binding.animeSummaryText.maxLines = if (isCurrentlyExpanded) {
                2
            } else {
                Int.MAX_VALUE
            }

            binding.animeGenresTagsCompact.isVisible = isCurrentlyExpanded
            binding.animeGenresTagsFullChips.isVisible = !isCurrentlyExpanded
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
