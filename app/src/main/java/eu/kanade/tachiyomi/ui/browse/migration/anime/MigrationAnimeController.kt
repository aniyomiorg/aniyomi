package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.databinding.MigrationMangaControllerBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.search.AnimeSearchController

class MigrationAnimeController :
    NucleusController<MigrationMangaControllerBinding, MigrationAnimePresenter>,
    FlexibleAdapter.OnItemClickListener,
    MigrationAnimeAdapter.OnCoverClickListener {

    private var adapter: MigrationAnimeAdapter? = null

    constructor(sourceId: Long, sourceName: String?) : super(
        bundleOf(
            SOURCE_ID_EXTRA to sourceId,
            SOURCE_NAME_EXTRA to sourceName
        )
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(
        bundle.getLong(SOURCE_ID_EXTRA),
        bundle.getString(SOURCE_NAME_EXTRA)
    )

    private val sourceId: Long = args.getLong(SOURCE_ID_EXTRA)
    private val sourceName: String? = args.getString(SOURCE_NAME_EXTRA)

    override fun getTitle(): String? {
        return sourceName
    }

    override fun createPresenter(): MigrationAnimePresenter {
        return MigrationAnimePresenter(sourceId)
    }

    override fun createBinding(inflater: LayoutInflater) = MigrationMangaControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        adapter = MigrationAnimeAdapter(this)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
        adapter?.fastScroller = binding.fastScroller
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    fun setAnime(anime: List<MigrationAnimeItem>) {
        adapter?.updateDataSet(anime)
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? MigrationAnimeItem ?: return false
        val controller = AnimeSearchController(item.anime)
        router.pushController(controller.withFadeTransaction())
        return false
    }

    override fun onCoverClick(position: Int) {
        val animeItem = adapter?.getItem(position) as? MigrationAnimeItem ?: return
        router.pushController(AnimeController(animeItem.anime).withFadeTransaction())
    }

    companion object {
        const val SOURCE_ID_EXTRA = "source_id_extra"
        const val SOURCE_NAME_EXTRA = "source_name_extra"
    }
}
