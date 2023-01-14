package eu.kanade.tachiyomi.ui.browse.migration.animesources

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.animebrowse.MigrateAnimeSourceScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.migration.anime.MigrationAnimeController

@Composable
fun migrateAnimeSourcesTab(
    router: Router?,
    presenter: MigrationAnimeSourcesPresenter,
): TabContent {
    val uriHandler = LocalUriHandler.current

    return TabContent(
        titleRes = R.string.label_migration_anime,
        actions = listOf(
            AppBar.Action(
                title = stringResource(R.string.migration_help_guide),
                icon = Icons.Outlined.HelpOutline,
                onClick = {
                    uriHandler.openUri("https://aniyomi.org/help/guides/source-migration/")
                },
            ),
        ),
        content = { contentPadding ->
            MigrateAnimeSourceScreen(
                presenter = presenter,
                contentPadding = contentPadding,
                onClickItem = { source ->
                    router?.pushController(
                        MigrationAnimeController(
                            source.id,
                            source.name,
                        ),
                    )
                },
            )
        },
    )
}
