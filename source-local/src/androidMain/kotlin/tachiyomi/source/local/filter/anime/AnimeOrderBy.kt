package tachiyomi.source.local.filter.anime

import android.content.Context
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import tachiyomi.core.i18n.localize
import tachiyomi.i18n.MR


sealed class AnimeOrderBy(context: Context, selection: Selection) : AnimeFilter.Sort(
    context.localize(MR.strings.local_filter_order_by),
    arrayOf(context.localize(MR.strings.title), context.localize(MR.strings.date)),
    selection,
) {
    class Popular(context: Context) : AnimeOrderBy(context, Selection(0, true))
    class Latest(context: Context) : AnimeOrderBy(context, Selection(1, false))
}
