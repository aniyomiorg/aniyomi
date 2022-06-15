package eu.kanade.tachiyomi.ui.setting.database

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.icon
import eu.kanade.tachiyomi.databinding.ClearDatabaseSourceItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.icon

class ClearDatabaseItemHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
    private val binding = ClearDatabaseSourceItemBinding.bind(view)

    fun bind(source: Source, count: Long) {
        binding.title.text = source.toString()
        binding.description.text = itemView.context.resources
            .getQuantityString(R.plurals.clear_database_source_item_count, count.toInt(), count.toInt())

        itemView.post {
            when {
                source.id == LocalSource.ID -> binding.thumbnail.setImageResource(R.mipmap.ic_local_source)
                source is SourceManager.StubSource -> binding.thumbnail.setImageDrawable(null)
                source.icon() != null -> binding.thumbnail.setImageDrawable(source.icon())
            }
        }
        binding.checkbox.isChecked = (bindingAdapter as FlexibleAdapter<*>).isSelected(bindingAdapterPosition)
    }

    fun bind(source: AnimeSource, count: Long) {
        binding.title.text = source.toString()
        binding.description.text = itemView.context.resources
            .getQuantityString(R.plurals.clear_database_source_item_count, count.toInt(), count.toInt())

        itemView.post {
            when {
                source.id == LocalAnimeSource.ID -> binding.thumbnail.setImageResource(R.mipmap.ic_local_source)
                source is AnimeSourceManager.StubSource -> binding.thumbnail.setImageDrawable(null)
                source.icon() != null -> binding.thumbnail.setImageDrawable(source.icon())
            }
        }
        binding.checkbox.isChecked = (bindingAdapter as FlexibleAdapter<*>).isSelected(bindingAdapterPosition)
    }
}
