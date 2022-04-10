package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import android.view.View
import androidx.core.view.isVisible
import coil.dispose
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.databinding.GlobalSearchControllerCardItemBinding
import eu.kanade.tachiyomi.util.view.loadAutoPause

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

        // Set anime title
        binding.title.text = anime.title

        // Set alpha of anime_thumbnail.
        binding.cover.alpha = if (anime.favorite) 0.3f else 1.0f

        // For rounded corners
        binding.badges.clipToOutline = true

        // Set favorite badge
        binding.favoriteText.isVisible = anime.favorite

        setImage(anime)
    }

    fun setImage(anime: Anime) {
        binding.cover.dispose()
        binding.cover.loadAutoPause(anime) {
            setParameter(AnimeCoverFetcher.USE_CUSTOM_COVER, false)
        }
    }
}
