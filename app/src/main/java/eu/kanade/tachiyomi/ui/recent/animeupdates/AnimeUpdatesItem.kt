package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodeItem
import eu.kanade.tachiyomi.ui.recent.DateSectionItem

class AnimeUpdatesItem(episode: Episode, val anime: Anime, header: DateSectionItem) :
    BaseEpisodeItem<AnimeUpdatesHolder, DateSectionItem>(episode, header) {

    override fun getLayoutRes(): Int {
        return R.layout.anime_updates_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): AnimeUpdatesHolder {
        return AnimeUpdatesHolder(view, adapter as AnimeUpdatesAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: AnimeUpdatesHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(this)
    }
}
