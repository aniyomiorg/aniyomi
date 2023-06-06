package tachiyomi.source.local.filter.manga

import android.content.Context
import eu.kanade.tachiyomi.source.model.Filter
import tachiyomi.source.local.R

sealed class MangaOrderBy(context: Context, selection: Selection) : Filter.Sort(
    context.getString(R.string.local_filter_order_by),
    arrayOf(context.getString(R.string.title), context.getString(R.string.date)),
    selection,
) {
    class Popular(context: Context) : MangaOrderBy(context, Selection(0, true))
    class Latest(context: Context) : MangaOrderBy(context, Selection(1, false))
}
