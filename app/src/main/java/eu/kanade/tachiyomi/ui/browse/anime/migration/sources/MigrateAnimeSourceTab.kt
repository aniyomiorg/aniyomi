package eu.kanade.tachiyomi.ui.browse.anime.migration.sources

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.MigrateAnimeSourceScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.anime.migration.anime.MigrationAnimeScreen

@Composable
fun Screen.migrateAnimeSourceTab(): TabContent {
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { MigrateAnimeSourceScreenModel() }
    val state by screenModel.state.collectAsState()

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
        content = { contentPadding, _ ->
            MigrateAnimeSourceScreen(
                state = state,
                contentPadding = contentPadding,
                onClickItem = { source -> navigator.push(MigrationAnimeScreen(source.id)) },
                onToggleSortingDirection = screenModel::toggleSortingDirection,
                onToggleSortingMode = screenModel::toggleSortingMode,
            )
        },
    )
}
