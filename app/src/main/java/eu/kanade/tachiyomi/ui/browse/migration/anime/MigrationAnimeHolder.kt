package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.view.View
import coil.dispose
import coil.load
import eu.davidea.viewholders.FlexibleViewHolder
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

        // Update the cover
        binding.thumbnail.dispose()
        binding.thumbnail.load(item.anime)
    }
}
