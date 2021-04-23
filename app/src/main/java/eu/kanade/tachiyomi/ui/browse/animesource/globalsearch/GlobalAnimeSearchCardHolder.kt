package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.glide.toAnimeThumbnail
import eu.kanade.tachiyomi.databinding.GlobalSearchControllerCardItemBinding
import eu.kanade.tachiyomi.widget.StateImageViewTarget

class GlobalAnimeSearchCardHolder(view: View, adapter: GlobalAnimeSearchCardAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = GlobalSearchControllerCardItemBinding.bind(view)

    init {
        // Call onAnimeClickListener when item is pressed.
        itemView.setOnClickListener {
            val item = adapter.getItem(bindingAdapterPosition)
            if (item != null) {
                adapter.animeClickListener.onAnimeClick(item.anime)
            }
        }
        itemView.setOnLongClickListener {
            val item = adapter.getItem(bindingAdapterPosition)
            if (item != null) {
                adapter.animeClickListener.onAnimeLongClick(item.anime)
            }
            true
        }
    }

    fun bind(anime: Anime) {
        binding.card.clipToOutline = true

        binding.title.text = anime.title
        // Set alpha of thumbnail.
        binding.cover.alpha = if (anime.favorite) 0.3f else 1.0f

        setImage(anime)
    }

    fun setImage(anime: Anime) {
        GlideApp.with(itemView.context).clear(binding.cover)
        if (!anime.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(itemView.context)
                .load(anime.toAnimeThumbnail())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .centerCrop()
                .skipMemoryCache(true)
                .placeholder(android.R.color.transparent)
                .into(StateImageViewTarget(binding.cover, binding.progress))
        }
    }
}
