package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.view.View
import androidx.core.view.isVisible
import coil.dispose
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.databinding.SourceCompactGridItemBinding
import eu.kanade.tachiyomi.util.view.loadAutoPause

/**
 * Class used to hold the displayed data of a anime in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_source_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
open class AnimeSourceCompactGridHolder(private val view: View, private val adapter: FlexibleAdapter<*>) :
    AnimeSourceHolder<SourceCompactGridItemBinding>(view, adapter) {

    override val binding = SourceCompactGridItemBinding.bind(view)

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
            setParameter(MangaCoverFetcher.USE_CUSTOM_COVER, false)
        }
    }
}
