package eu.kanade.tachiyomi.ui.recent.animehistory

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Adapter of AnimeHistoryHolder.
 * Connection between Fragment and Holder
 * Holder updates should be called from here.
 *
 * @param controller a AnimeHistoryController object
 * @constructor creates an instance of the adapter.
 */
class AnimeHistoryAdapter(controller: AnimeHistoryController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val sourceManager: AnimeSourceManager by injectLazy()

    val resumeClickListener: OnResumeClickListener = controller
    val removeClickListener: OnRemoveClickListener = controller
    val itemClickListener: OnItemClickListener = controller

    /**
     * DecimalFormat used to display correct chapter number
     */
    val decimalFormat = DecimalFormat(
        "#.###",
        DecimalFormatSymbols()
            .apply { decimalSeparator = '.' },
    )

    init {
        setDisplayHeadersAtStartUp(true)
    }

    interface OnResumeClickListener {
        fun onResumeClick(position: Int)
    }

    interface OnRemoveClickListener {
        fun onRemoveClick(position: Int)
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
