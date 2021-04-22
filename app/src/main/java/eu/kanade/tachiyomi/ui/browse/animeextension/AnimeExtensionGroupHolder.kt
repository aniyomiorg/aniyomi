package eu.kanade.tachiyomi.ui.browse.animeextension

import android.annotation.SuppressLint
import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.databinding.SectionHeaderItemBinding

class AnimeExtensionGroupHolder(view: View, adapter: FlexibleAdapter<*>) :
    FlexibleViewHolder(view, adapter) {

    private val binding = SectionHeaderItemBinding.bind(view)

    @SuppressLint("SetTextI18n")
    fun bind(item: AnimeExtensionGroupItem) {
        var text = item.name
        if (item.showSize) {
            text += " (${item.size})"
        }

        binding.title.text = text
    }
}
