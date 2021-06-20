package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.view.View
import coil.clear
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.databinding.SourceListItemBinding
import eu.kanade.tachiyomi.util.system.getResourceColor

/**
 * Class used to hold the displayed data of a anime in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_catalogue_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class AnimeSourceListHolder(private val view: View, adapter: FlexibleAdapter<*>) :
    AnimeSourceHolder<SourceListItemBinding>(view, adapter) {

    override val binding = SourceListItemBinding.bind(view)

    private val favoriteColor = view.context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    private val unfavoriteColor = view.context.getResourceColor(R.attr.colorOnSurface)

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given anime.
     *
     * @param anime the anime to bind.
     */
    override fun onSetValues(anime: Anime) {
        binding.title.text = anime.title
        binding.title.setTextColor(if (anime.favorite) favoriteColor else unfavoriteColor)

        // Set alpha of thumbnail.
        binding.thumbnail.alpha = if (anime.favorite) 0.3f else 1.0f

        setImage(anime)
    }

    override fun setImage(anime: Anime) {
        binding.thumbnail.clear()
        if (!anime.thumbnail_url.isNullOrEmpty()) {
            val radius = view.context.resources.getDimension(R.dimen.card_radius)
            binding.thumbnail.loadAny(anime) {
                setParameter(AnimeCoverFetcher.USE_CUSTOM_COVER, false)
                transformations(RoundedCornersTransformation(radius))
            }
        }
    }
}
