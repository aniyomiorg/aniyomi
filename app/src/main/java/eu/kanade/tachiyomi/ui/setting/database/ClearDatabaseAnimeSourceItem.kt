package eu.kanade.tachiyomi.ui.setting.database
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.icon
import eu.kanade.tachiyomi.databinding.ClearDatabaseSourceItemBinding

data class ClearDatabaseAnimeSourceItem(val source: AnimeSource, private val mangaCount: Long) : AbstractFlexibleItem<ClearDatabaseAnimeSourceItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.clear_database_source_item
    }
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }
    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: Holder?, position: Int, payloads: MutableList<Any>?) {
        holder?.bind(source, mangaCount)
    }
    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        private val binding = ClearDatabaseSourceItemBinding.bind(view)

        fun bind(source: AnimeSource, count: Long) {
            binding.title.text = source.toString()
            binding.description.text = itemView.context.resources
                .getQuantityString(R.plurals.clear_database_source_item_count, count.toInt())

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
}
