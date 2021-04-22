package eu.kanade.tachiyomi.ui.browse.source.browse

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toAnimeThumbnail
import eu.kanade.tachiyomi.databinding.AnimeSourceListItemBinding
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
        AnimeSourceHolder<AnimeSourceListItemBinding>(view, adapter) {

    override val binding = AnimeSourceListItemBinding.bind(view)

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
        GlideApp.with(view.context).clear(binding.thumbnail)

        if (!anime.thumbnail_url.isNullOrEmpty()) {
            val radius = view.context.resources.getDimensionPixelSize(R.dimen.card_radius)
            val requestOptions = RequestOptions().transform(CenterCrop(), RoundedCorners(radius))
            GlideApp.with(view.context)
                .load(anime.toAnimeThumbnail())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .apply(requestOptions)
                .dontAnimate()
                .placeholder(android.R.color.transparent)
                .into(binding.thumbnail)
        }
    }
}
