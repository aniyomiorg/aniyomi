package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import android.view.View
import coil.clear
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher
import eu.kanade.tachiyomi.data.database.models.Anime
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
        binding.cover.clear()
        if (!anime.thumbnail_url.isNullOrEmpty()) {
            val crossfadeDuration = itemView.context.imageLoader.defaults.transition.let {
                if (it is CrossfadeTransition) it.durationMillis else 0
            }
            val request = ImageRequest.Builder(itemView.context)
                .data(anime)
                .setParameter(AnimeCoverFetcher.USE_CUSTOM_COVER, false)
                .diskCachePolicy(CachePolicy.DISABLED)
                .target(StateImageViewTarget(binding.cover, binding.progress, crossfadeDuration))
                .build()
            itemView.context.imageLoader.enqueue(request)
        }
    }
}
