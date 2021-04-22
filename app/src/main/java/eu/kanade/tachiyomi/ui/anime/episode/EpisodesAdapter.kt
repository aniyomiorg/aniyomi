package eu.kanade.tachiyomi.ui.anime.episode

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodesAdapter
import eu.kanade.tachiyomi.util.system.getResourceColor
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class EpisodesAdapter(
    controller: AnimeController,
    context: Context
) : BaseEpisodesAdapter<EpisodeItem>(controller) {

    private val preferences: PreferencesHelper by injectLazy()

    var items: List<EpisodeItem> = emptyList()

    val readColor = context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    val unreadColor = context.getResourceColor(R.attr.colorOnSurface)
    val unreadColorSecondary = context.getResourceColor(android.R.attr.textColorSecondary)

    val bookmarkedColor = context.getResourceColor(R.attr.colorAccent)

    val decimalFormat = DecimalFormat(
        "#.###",
        DecimalFormatSymbols()
            .apply { decimalSeparator = '.' }
    )

    val dateFormat: DateFormat = preferences.dateFormat()

    override fun updateDataSet(items: List<EpisodeItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun indexOf(item: EpisodeItem): Int {
        return items.indexOf(item)
    }
}
