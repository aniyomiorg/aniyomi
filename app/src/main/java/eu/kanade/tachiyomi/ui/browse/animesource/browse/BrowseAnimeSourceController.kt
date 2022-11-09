package eu.kanade.tachiyomi.ui.browse.animesource.browse

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.os.bundleOf
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.presentation.animebrowse.BrowseAnimeSourceScreen
import eu.kanade.presentation.animebrowse.components.RemoveAnimeDialog
import eu.kanade.presentation.components.ChangeCategoryDialog
import eu.kanade.presentation.components.DuplicateAnimeDialog
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.animecategory.AnimeCategoryController
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourcePresenter.Dialog
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.lang.launchIO

open class BrowseAnimeSourceController(bundle: Bundle) :
    FullComposeController<BrowseAnimeSourcePresenter>(bundle) {

    constructor(sourceId: Long, query: String? = null) : this(
        bundleOf(
            SOURCE_ID_KEY to sourceId,
            SEARCH_QUERY_KEY to query,
        ),
    )

    constructor(source: AnimeCatalogueSource, query: String? = null) : this(source.id, query)

    constructor(source: AnimeSource, query: String? = null) : this(source.id, query)

    /**
     * Sheet containing filter items.
     */
    protected var filterSheet: AnimeSourceFilterSheet? = null

    override fun createPresenter(): BrowseAnimeSourcePresenter {
        return BrowseAnimeSourcePresenter(args.getLong(SOURCE_ID_KEY), args.getString(SEARCH_QUERY_KEY))
    }

    @Composable
    override fun ComposeContent() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current

        BrowseAnimeSourceScreen(
            presenter = presenter,
            navigateUp = ::navigateUp,
            openFilterSheet = { filterSheet?.show() },
            onAnimeClick = { router.pushController(AnimeController(it.id, true)) },
            onAnimeLongClick = { anime ->
                scope.launchIO {
                    val duplicateAnime = presenter.getDuplicateLibraryAnime(anime)
                    when {
                        anime.favorite -> presenter.dialog = Dialog.RemoveAnime(anime)
                        duplicateAnime != null -> presenter.dialog = Dialog.AddDuplicateAnime(anime, duplicateAnime)
                        else -> presenter.addFavorite(anime)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onWebViewClick = f@{
                val source = presenter.source as? AnimeHttpSource ?: return@f
                val intent = WebViewActivity.newIntent(context, source.baseUrl, source.id, source.name)
                context.startActivity(intent)
            },
            incognitoMode = presenter.isIncognitoMode,
            downloadedOnlyMode = presenter.isDownloadOnly,
        )

        val onDismissRequest = { presenter.dialog = null }
        when (val dialog = presenter.dialog) {
            is Dialog.AddDuplicateAnime -> {
                DuplicateAnimeDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { presenter.addFavorite(dialog.anime) },
                    onOpenAnime = { router.pushController(AnimeController(dialog.duplicate.id)) },
                    duplicateFrom = presenter.getSourceOrStub(dialog.duplicate),
                )
            }
            is Dialog.RemoveAnime -> {
                RemoveAnimeDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        presenter.changeAnimeFavorite(dialog.anime)
                    },
                    animeToRemove = dialog.anime,
                )
            }
            is Dialog.ChangeAnimeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        router.pushController(AnimeCategoryController())
                    },
                    onConfirm = { include, _ ->
                        presenter.changeAnimeFavorite(dialog.anime)
                        presenter.moveAnimeToCategories(dialog.anime, include)
                    },
                )
            }
            null -> {}
        }

        BackHandler(onBack = ::navigateUp)

        LaunchedEffect(presenter.filters) {
            initFilterSheet()
        }
    }

    private fun navigateUp() {
        when {
            !presenter.isUserQuery && presenter.searchQuery != null -> presenter.searchQuery = null
            else -> router.popCurrentController()
        }
    }

    open fun initFilterSheet() {
        if (presenter.filters.isEmpty()) {
            return
        }

        filterSheet = AnimeSourceFilterSheet(
            activity!!,
            onFilterClicked = {
                presenter.search(filters = presenter.filters)
            },
            onResetClicked = {
                presenter.reset()
                filterSheet?.setFilters(presenter.filterItems)
            },
        )

        filterSheet?.setFilters(presenter.filterItems)
    }

    /**
     * Restarts the request with a new query.
     *
     * @param newQuery the new query.
     */
    fun searchWithQuery(newQuery: String) {
        presenter.search(newQuery)
    }

    /**
     * Attempts to restart the request with a new genre-filtered query.
     * If the genre name can't be found the filters,
     * the standard searchWithQuery search method is used instead.
     *
     * @param genreName the name of the genre
     */
    fun searchWithGenre(genreName: String) {
        val defaultFilters = presenter.source!!.getFilterList()

        var genreExists = false

        filter@ for (sourceFilter in defaultFilters) {
            if (sourceFilter is AnimeFilter.Group<*>) {
                for (filter in sourceFilter.state) {
                    if (filter is AnimeFilter<*> && filter.name.equals(genreName, true)) {
                        when (filter) {
                            is AnimeFilter.TriState -> filter.state = 1
                            is AnimeFilter.CheckBox -> filter.state = true
                            else -> {}
                        }
                        genreExists = true
                        break@filter
                    }
                }
            } else if (sourceFilter is AnimeFilter.Select<*>) {
                val index = sourceFilter.values.filterIsInstance<String>()
                    .indexOfFirst { it.equals(genreName, true) }

                if (index != -1) {
                    sourceFilter.state = index
                    genreExists = true
                    break
                }
            }
        }

        if (genreExists) {
            filterSheet?.setFilters(defaultFilters.toItems())

            presenter.search(filters = defaultFilters)
        } else {
            searchWithQuery(genreName)
        }
    }

    protected companion object {
        const val SOURCE_ID_KEY = "sourceId"
        const val SEARCH_QUERY_KEY = "searchQuery"
    }
}
