package eu.kanade.tachiyomi.ui.browse.migration.animesources

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.presentation.animesource.MigrateAnimeSourceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.ComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.migration.anime.MigrationAnimeController
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.openInBrowser

class MigrationAnimeSourcesController : ComposeController<MigrationAnimeSourcesPresenter>() {

    init {
        setHasOptionsMenu(true)
    }

    override fun createPresenter() = MigrationAnimeSourcesPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        MigrateAnimeSourceScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickItem = { source ->
                parentController!!.router.pushController(
                    MigrationAnimeController(
                        source.id,
                        source.name
                    )
                )
            },
            onLongClickItem = { source ->
                val sourceId = source.id.toString()
                activity?.copyToClipboard(sourceId, sourceId)
            },
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
        inflater.inflate(R.menu.browse_migrate, menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (val itemId = item.itemId) {
            R.id.action_source_migration_help -> {
                activity?.openInBrowser(HELP_URL)
                true
            }
            R.id.asc_alphabetical,
            R.id.desc_alphabetical -> {
                presenter.setAlphabeticalSorting(itemId == R.id.asc_alphabetical)
                true
            }
            R.id.asc_count,
            R.id.desc_count -> {
                presenter.setTotalSorting(itemId == R.id.asc_count)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

private const val HELP_URL = "https://aniyomi.jmir.xyz/help/guides/source-migration/"
