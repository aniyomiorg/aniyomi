package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.view.View
import androidx.core.view.isVisible
import coil.clear
import coil.imageLoader
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.databinding.SourceComfortableGridItemBinding
import eu.kanade.tachiyomi.widget.StateImageViewTarget

/**
 * Class used to hold the displayed data of a anime in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_source_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class AnimeSourceComfortableGridHolder(private val view: View, private val adapter: FlexibleAdapter<*>) :
    AnimeSourceHolder<SourceComfortableGridItemBinding>(view, adapter) {

    override val binding = SourceComfortableGridItemBinding.bind(view)

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given anime.
     *
     * @param anime the anime to bind.
     */
    override fun onSetValues(anime: Anime) {
        // Set anime title
        binding.title.text = anime.title

        // Set alpha of thumbnail.
        binding.thumbnail.alpha = if (anime.favorite) 0.3f else 1.0f

        // For rounded corners
        binding.badges.leftBadges.clipToOutline = true
        binding.badges.rightBadges.clipToOutline = true

        // Set favorite badge
        binding.badges.favoriteText.isVisible = anime.favorite

        setImage(anime)
    }

    override fun setImage(anime: Anime) {
        // For rounded corners
        binding.card.clipToOutline = true

        binding.thumbnail.clear()
        if (!anime.thumbnail_url.isNullOrEmpty()) {
            val crossfadeDuration = view.context.imageLoader.defaults.transition.let {
                if (it is CrossfadeTransition) it.durationMillis else 0
            }
            val request = ImageRequest.Builder(view.context)
                .data(anime)
                .setParameter(AnimeCoverFetcher.USE_CUSTOM_COVER, false)
                .target(StateImageViewTarget(binding.thumbnail, binding.progress, crossfadeDuration))
                .build()
            itemView.context.imageLoader.enqueue(request)
        }
    }
}
