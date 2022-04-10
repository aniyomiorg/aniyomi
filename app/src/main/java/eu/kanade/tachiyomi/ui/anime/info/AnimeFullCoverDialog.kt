package eu.kanade.tachiyomi.ui.anime.info

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.databinding.MangaFullCoverDialogBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.util.view.setNavigationBarTransparentCompat
import eu.kanade.tachiyomi.widget.TachiyomiFullscreenDialog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeFullCoverDialog : DialogController {

    private var anime: Anime? = null

    private var binding: MangaFullCoverDialogBinding? = null

    private var disposable: Disposable? = null

    private val animeController
        get() = targetController as AnimeController?

    constructor(targetController: AnimeController, anime: Anime) : super(bundleOf("animeId" to anime.id)) {
        this.targetController = targetController
        this.anime = anime
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        val db = Injekt.get<AnimeDatabaseHelper>()
        anime = db.getAnime(bundle.getLong("animeId")).executeAsBlocking()
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = MangaFullCoverDialogBinding.inflate(activity!!.layoutInflater)

        binding?.toolbar?.apply {
            setNavigationOnClickListener { dialog?.dismiss() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_share_cover -> animeController?.shareCover()
                    R.id.action_save_cover -> animeController?.saveCover()
                    R.id.action_edit_cover -> animeController?.changeCover()
                }
                true
            }
            menu?.findItem(R.id.action_edit_cover)?.isVisible = anime?.favorite ?: false
        }

        setImage(anime)

        binding?.appbar?.applyInsetter {
            type(navigationBars = true, statusBars = true) {
                padding(left = true, top = true, right = true)
            }
        }

        binding?.container?.onViewClicked = { dialog?.dismiss() }
        binding?.container?.applyInsetter {
            type(navigationBars = true) {
                padding(bottom = true)
            }
        }

        return TachiyomiFullscreenDialog(activity!!, binding!!.root).apply {
            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
            window?.setBackgroundDrawable(ColorDrawable(ColorUtils.setAlphaComponent(typedValue.data, 230)))
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        dialog?.window?.let { window ->
            window.setNavigationBarTransparentCompat(window.context)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        disposable?.dispose()
        disposable = null
    }

    fun setImage(anime: Anime?) {
        if (anime == null) return
        val request = ImageRequest.Builder(applicationContext!!)
            .data(anime)
            .target {
                binding?.container?.setImage(
                    it,
                    ReaderPageImageView.Config(
                        zoomDuration = 500,
                    ),
                )
            }
            .build()

        disposable = applicationContext?.imageLoader?.enqueue(request)
    }
}
