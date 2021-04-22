package eu.kanade.tachiyomi.ui.anime.episode

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.DialogCustomDownloadView

/**
 * Dialog used to let user select amount of episodes to download.
 */
class DownloadCustomEpisodesDialog<T> : DialogController
        where T : Controller, T : DownloadCustomEpisodesDialog.Listener {

    /**
     * Maximum number of episodes to download in download chooser.
     */
    private val maxEpisodes: Int

    /**
     * Initialize dialog.
     * @param maxEpisodes maximal number of episodes that user can download.
     */
    constructor(target: T, maxEpisodes: Int) : super(
        // Add maximum number of episodes to download value to bundle.
        bundleOf(KEY_ITEM_MAX to maxEpisodes)
    ) {
        targetController = target
        this.maxEpisodes = maxEpisodes
    }

    /**
     * Restore dialog.
     * @param bundle bundle containing data from state restore.
     */
    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        // Get maximum episodes to download from bundle
        val maxEpisodes = bundle.getInt(KEY_ITEM_MAX, 0)
        this.maxEpisodes = maxEpisodes
    }

    /**
     * Called when dialog is being created.
     */
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!

        // Initialize view that lets user select number of episodes to download.
        val view = DialogCustomDownloadView(activity).apply {
            setMinMax(0, maxEpisodes)
        }

        // Build dialog.
        // when positive dialog is pressed call custom listener.
        return MaterialDialog(activity)
            .title(R.string.custom_download)
            .customView(view = view, scrollable = true)
            .positiveButton(android.R.string.ok) {
                (targetController as? Listener)?.downloadCustomEpisodes(view.amount)
            }
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun downloadCustomEpisodes(amount: Int)
    }

    private companion object {
        // Key to retrieve max episodes from bundle on process death.
        const val KEY_ITEM_MAX = "DownloadCustomEpisodesDialog.int.maxEpisodes"
    }
}
