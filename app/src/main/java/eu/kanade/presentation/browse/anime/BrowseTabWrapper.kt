package eu.kanade.presentation.browse.anime

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.TabContent
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BrowseTabWrapper(tab: TabContent) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navigator = LocalNavigator.current
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(tab.titleRes),
                actions = {
                    AppBarActions(tab.actions)
                },
                scrollBehavior = scrollBehavior,
                navigateUp = { navigator?.pop() },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        tab.content(paddingValues, snackbarHostState)
    }
}
