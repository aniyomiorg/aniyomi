package eu.kanade.tachiyomi.ui.browse.migration.anime

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import eu.kanade.presentation.animebrowse.MigrateAnimeScreen
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.migration.search.AnimeSearchController

class MigrationAnimeController : FullComposeController<MigrateAnimePresenter> {

    constructor(sourceId: Long, sourceName: String?) : super(
        bundleOf(
            SOURCE_ID_EXTRA to sourceId,
            SOURCE_NAME_EXTRA to sourceName,
        ),
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(
        bundle.getLong(SOURCE_ID_EXTRA),
        bundle.getString(SOURCE_NAME_EXTRA),
    )

    private val sourceId: Long = args.getLong(SOURCE_ID_EXTRA)
    private val sourceName: String? = args.getString(SOURCE_NAME_EXTRA)

    override fun createPresenter() = MigrateAnimePresenter(sourceId)

    @Composable
    override fun ComposeContent() {
        MigrateAnimeScreen(
            navigateUp = router::popCurrentController,
            title = sourceName,
            presenter = presenter,
            onClickItem = {
                router.pushController(AnimeSearchController(it.id))
            },
            onClickCover = {
                router.pushController(AnimeController(it.id))
            },
        )
    }

    companion object {
        const val SOURCE_ID_EXTRA = "source_id_extra"
        const val SOURCE_NAME_EXTRA = "source_name_extra"
    }
}
