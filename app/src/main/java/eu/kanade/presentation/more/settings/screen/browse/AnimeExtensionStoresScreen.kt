package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.browse.components.anime.AnimeExtensionStoreConfirmDialog
import eu.kanade.presentation.more.settings.screen.browse.components.anime.AnimeExtensionStoreCreateDialog
import eu.kanade.presentation.more.settings.screen.browse.components.anime.AnimeExtensionStoreDeleteDialog
import eu.kanade.presentation.more.settings.screen.browse.components.anime.AnimeExtensionStoresScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.openInBrowser
import tachiyomi.presentation.core.screens.LoadingScreen

class AnimeExtensionStoresScreen(
    private val url: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { AnimeExtensionStoresScreenModel() }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(url) {
            url?.let { screenModel.addFromDeeplink(url) }
        }

        if (state is AnimeExtensionStoreScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as AnimeExtensionStoreScreenState.Success

        AnimeExtensionStoresScreen(
            state = successState,
            onClickCreate = { screenModel.showDialog(AnimeExtensionStoreDialog.Create()) },
            onCopy = { context.copyToClipboard(it.indexUrl, it.indexUrl) },
            onOpenWebsite = { it.contact.website.let(context::openInBrowser) },
            onOpenDiscord = { it.contact.discord?.let(context::openInBrowser) },
            onClickDelete = { screenModel.showDialog(AnimeExtensionStoreDialog.Delete(it)) },
            onClickRefresh = { screenModel.refreshRepos() },
            navigateUp = navigator::pop,
        )

        when (val dialog = successState.dialog) {
            null -> {}
            is AnimeExtensionStoreDialog.Create -> {
                AnimeExtensionStoreCreateDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onCreate = { screenModel.createRepo(it) },
                    storeIndexUrls = successState.stores.map { it.indexUrl }.toSet(),
                    processing = dialog.processing,
                    errorMessage = dialog.errorMessage,
                )
            }
            is AnimeExtensionStoreDialog.Delete -> {
                AnimeExtensionStoreDeleteDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onDelete = { screenModel.deleteRepo(dialog.store.indexUrl) },
                    storeName = dialog.store.name,
                    storeIndexUrl = dialog.store.indexUrl,
                )
            }
            is AnimeExtensionStoreDialog.Confirm -> {
                AnimeExtensionStoreConfirmDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onCreate = { screenModel.createRepo(dialog.url) },
                    storeIndexUrl = dialog.url,
                    storeAlreadyExists = dialog.alreadyExists,
                    processing = dialog.processing,
                    errorMessage = dialog.errorMessage,
                )
            }
        }
    }
}
