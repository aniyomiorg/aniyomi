package tachiyomi.source.local.filter.anime

import android.content.Context
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import tachiyomi.source.local.R

sealed class AnimeOrderBy(context: Context, selection: Selection) : AnimeFilter.Sort(
    context.getString(R.string.local_filter_order_by),
    arrayOf(context.getString(R.string.title), context.getString(R.string.date)),
    selection,
) {
    class Popular(context: Context) : AnimeOrderBy(context, Selection(0, true))
    class Latest(context: Context) : AnimeOrderBy(context, Selection(1, false))
}
