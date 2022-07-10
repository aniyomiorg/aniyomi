package eu.kanade.tachiyomi.ui.browse.animesource.browse

import androidx.core.view.isVisible
import coil.dispose
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher
import eu.kanade.tachiyomi.databinding.SourceCompactGridItemBinding
import eu.kanade.tachiyomi.util.view.loadAutoPause

/**
 * Class used to hold the displayed data of a anime in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_source_grid" are available in this class.
 *
 * @param binding the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class AnimeSourceCompactGridHolder(
    override val binding: SourceCompactGridItemBinding,
    adapter: FlexibleAdapter<*>,
) : AnimeSourceHolder<SourceCompactGridItemBinding>(binding.root, adapter) {

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
        binding.thumbnail.dispose()
        binding.thumbnail.loadAutoPause(anime) {
            setParameter(AnimeCoverFetcher.USE_CUSTOM_COVER, false)
        }
    }
}
