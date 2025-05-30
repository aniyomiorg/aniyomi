package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.AnimeExtensionReposScreen
import eu.kanade.presentation.more.settings.screen.browse.MangaExtensionReposScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import kotlinx.collections.immutable.persistentListOf
import mihon.domain.extensionrepo.anime.interactor.GetAnimeExtensionRepoCount
import mihon.domain.extensionrepo.manga.interactor.GetMangaExtensionRepoCount
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val getMangaExtensionRepoCount = remember { Injekt.get<GetMangaExtensionRepoCount>() }
        val getAnimeExtensionRepoCount = remember { Injekt.get<GetAnimeExtensionRepoCount>() }

        val mangaReposCount by getMangaExtensionRepoCount.subscribe().collectAsState(0)
        val animeReposCount by getAnimeExtensionRepoCount.subscribe().collectAsState(0)

        // SY -->
        val scope = rememberCoroutineScope()
        val hideFeedTab by remember { Injekt.get<UiPreferences>().hideFeedTab().asState(scope) }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        // SY <--

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = persistentListOf(
                    // KMK -->
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.relatedAnimes(),
                        title = stringResource(TLMR.strings.pref_source_related_animes),
                        subtitle = stringResource(TLMR.strings.pref_source_related_animes_summary),
                    ),
                    // KMK <--
//                    kotlin.run {
//                        val count by sourcePreferences.sourcesTabCategories().collectAsState()
//                        Preference.PreferenceItem.TextPreference(
//                            title = stringResource(MR.strings.action_edit_categories),
//                            subtitle = pluralStringResource(MR.plurals.num_categories, count.size, count.size),
//                            onClick = {
//                                navigator.push(SourceCategoryScreen())
//                            },
//                        )
//                    },
//                    Preference.PreferenceItem.SwitchPreference(
//                        pref = sourcePreferences.sourcesTabCategoriesFilter(),
//                        title = stringResource(SYMR.strings.pref_source_source_filtering),
//                        subtitle = stringResource(SYMR.strings.pref_source_source_filtering_summery),
//                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = uiPreferences.useNewSourceNavigation(),
                        title = stringResource(TLMR.strings.pref_source_navigation),
                        subtitle = stringResource(TLMR.strings.pref_source_navigation_summery),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(TLMR.strings.feed),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = uiPreferences.hideFeedTab(),
                        title = stringResource(TLMR.strings.pref_hide_feed),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = uiPreferences.feedTabInFront(),
                        title = stringResource(TLMR.strings.pref_feed_position),
                        subtitle = stringResource(TLMR.strings.pref_feed_position_summery),
                        enabled = hideFeedTab.not(),
                    ),
                    // KMK -->
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.hideInLibraryFeedItems(),
                        title = stringResource(MR.strings.pref_hide_in_library_items),
                    ),
                    // KMK <--
                ),
            ),
            // SY <--

            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInAnimeLibraryItems(),
                        title = stringResource(MR.strings.pref_hide_in_anime_library_items),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInMangaLibraryItems(),
                        title = stringResource(MR.strings.pref_hide_in_manga_library_items),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_anime_extension_repos),
                        subtitle = pluralStringResource(
                            MR.plurals.num_repos,
                            animeReposCount,
                            animeReposCount,
                        ),
                        onClick = {
                            navigator.push(AnimeExtensionReposScreen())
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_manga_extension_repos),
                        subtitle = pluralStringResource(
                            MR.plurals.num_repos,
                            mangaReposCount,
                            mangaReposCount,
                        ),
                        onClick = {
                            navigator.push(MangaExtensionReposScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_nsfw_content),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.showNsfwSource(),
                        title = stringResource(MR.strings.pref_show_nsfw_source),
                        subtitle = stringResource(MR.strings.requires_app_restart),
                        onValueChanged = {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(MR.strings.pref_category_nsfw_content),
                            )
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(MR.strings.parental_controls_info),
                    ),
                ),
            ),
        )
    }
}
