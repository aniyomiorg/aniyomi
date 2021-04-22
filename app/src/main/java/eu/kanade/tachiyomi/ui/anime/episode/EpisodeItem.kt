package eu.kanade.tachiyomi.ui.anime.episode

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodeItem

class EpisodeItem(episode: Episode, val anime: Anime) :
    BaseEpisodeItem<EpisodeHolder, AbstractHeaderItem<FlexibleViewHolder>>(episode) {

    override fun getLayoutRes(): Int {
        return R.layout.episodes_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): EpisodeHolder {
        return EpisodeHolder(view, adapter as EpisodesAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: EpisodeHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(this, anime)
    }
}
