package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.view.View
import coil.clear
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.SourceListItemBinding

class MigrationAnimeHolder(
    view: View,
    private val adapter: MigrationAnimeAdapter
) : FlexibleViewHolder(view, adapter) {

    private val binding = SourceListItemBinding.bind(view)

    init {
        binding.thumbnail.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }
    }

    fun bind(item: MigrationAnimeItem) {
        binding.title.text = item.anime.title

        // Update the cover.
        val radius = itemView.context.resources.getDimension(R.dimen.card_radius)
        binding.thumbnail.clear()
        binding.thumbnail.loadAny(item.anime) {
            transformations(RoundedCornersTransformation(radius))
        }
    }
}
