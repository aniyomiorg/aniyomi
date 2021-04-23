package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toAnimeThumbnail
import eu.kanade.tachiyomi.databinding.AnimeSourceComfortableGridItemBinding
import eu.kanade.tachiyomi.widget.StateImageViewTarget

/**
 * Class used to hold the displayed data of a anime in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_source_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
open class AnimeSourceGridHolder(private val view: View, private val adapter: FlexibleAdapter<*>) :
    AnimeSourceHolder<AnimeSourceComfortableGridItemBinding>(view, adapter) {

    override val binding = AnimeSourceComfortableGridItemBinding.bind(view)

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

        setImage(anime)
    }

    override fun setImage(anime: Anime) {
        // For rounded corners
        binding.card.clipToOutline = true

        GlideApp.with(view.context).clear(binding.thumbnail)
        if (!anime.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(view.context)
                .load(anime.toAnimeThumbnail())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .centerCrop()
                .placeholder(android.R.color.transparent)
                .into(StateImageViewTarget(binding.thumbnail, binding.progress))
        }
    }
}
