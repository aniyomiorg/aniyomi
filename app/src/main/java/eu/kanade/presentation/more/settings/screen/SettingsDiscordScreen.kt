// AM (DISCORD) -->
package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastMap
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsDiscordScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = TLMR.strings.pref_category_connections

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(onClick = { uriHandler.openUri("https://tachiyomi.org/help/guides/tracking/") }) {
            Icon(
                imageVector = Icons.Outlined.HelpOutline,
                contentDescription = stringResource(R.string.tracking_guide),
            )
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val connectionsPreferences = remember { Injekt.get<ConnectionsPreferences>() }
        val connectionsManager = remember { Injekt.get<ConnectionsManager>() }
        val enableDRPCPref = connectionsPreferences.enableDiscordRPC()
        val useChapterTitlesPref = connectionsPreferences.useChapterTitles()
        val discordRPCStatus = connectionsPreferences.discordRPCStatus()
        val customMessagePref = connectionsPreferences.discordCustomMessage()
        val showProgressPref = connectionsPreferences.discordShowProgress()
        val showTimestampPref = connectionsPreferences.discordShowTimestamp()
        val showButtonsPref = connectionsPreferences.discordShowButtons()
        val showDownloadButtonPref = connectionsPreferences.discordShowDownloadButton()
        val showDiscordButtonPref = connectionsPreferences.discordShowDiscordButton()

        val enableDRPC by enableDRPCPref.collectAsState()
        val useChapterTitles by useChapterTitlesPref.collectAsState()
        val showButtons by showButtonsPref.collectAsState()

        var dialog by remember { mutableStateOf<Any?>(null) }
        dialog?.run {
            when (this) {
                is LogoutConnectionsDialog -> {
                    ConnectionsLogoutDialog(
                        service = service,
                        onDismissRequest = {
                            dialog = null
                            enableDRPCPref.set(false)
                        },
                    )
                }
            }
        }

        var showCustomMessageDialog by rememberSaveable { mutableStateOf(false) }
        var tempCustomMessage by rememberSaveable { mutableStateOf(customMessagePref.get()) }

        if (showCustomMessageDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCustomMessageDialog = false
                    tempCustomMessage = customMessagePref.get()
                },
                title = { Text(stringResource(R.string.pref_discord_custom_message)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tempCustomMessage,
                            onValueChange = { tempCustomMessage = it },
                            label = { Text(stringResource(R.string.pref_discord_custom_message_summary)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        TextButton(
                            onClick = {
                                customMessagePref.delete()
                                tempCustomMessage = ""
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(stringResource(R.string.action_reset))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            customMessagePref.set(tempCustomMessage)
                            showCustomMessageDialog = false
                        },
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCustomMessageDialog = false
                            tempCustomMessage = customMessagePref.get()
                        },
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.connections_discord),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = enableDRPCPref,
                        title = stringResource(R.string.pref_enable_discord_rpc),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = useChapterTitlesPref,
                        enabled = enableDRPC,
                        title = stringResource(id = R.string.show_chapters_titles_title),
                        subtitle = stringResource(id = R.string.show_chapters_titles_subtitle),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = discordRPCStatus,
                        title = stringResource(R.string.pref_discord_status),
                        entries = persistentMapOf(
                            -1 to stringResource(R.string.pref_discord_dnd),
                            0 to stringResource(R.string.pref_discord_idle),
                            1 to stringResource(R.string.pref_discord_online),
                        ),
                        enabled = enableDRPC,
                    ),
                ),
            ),
            getRPCIncognitoGroup(
                connectionsPreferences = connectionsPreferences,
                enabled = enableDRPC,
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.pref_category_discord_customization),
                enabled = enableDRPC,
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.pref_discord_custom_message),
                        subtitle = stringResource(R.string.pref_discord_custom_message_summary),
                        onClick = { showCustomMessageDialog = true },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = showProgressPref,
                        title = stringResource(R.string.pref_discord_show_progress),
                        subtitle = stringResource(R.string.pref_discord_show_progress_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = showTimestampPref,
                        title = stringResource(R.string.pref_discord_show_timestamp),
                        subtitle = stringResource(R.string.pref_discord_show_timestamp_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = showButtonsPref,
                        title = stringResource(R.string.pref_discord_show_buttons),
                        subtitle = stringResource(R.string.pref_discord_show_buttons_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = showDownloadButtonPref,
                        title = stringResource(R.string.pref_discord_show_download_button),
                        subtitle = stringResource(R.string.pref_discord_show_download_button_summary),
                        enabled = showButtons,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = showDiscordButtonPref,
                        title = stringResource(R.string.pref_discord_show_discord_button),
                        subtitle = stringResource(R.string.pref_discord_show_discord_button_summary),
                        enabled = showButtons,
                    ),
                ),
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.logout),
                onClick = { dialog = LogoutConnectionsDialog(connectionsManager.discord) },
            ),
        )
    }

    @Composable
    private fun getRPCIncognitoGroup(
        connectionsPreferences: ConnectionsPreferences,
        enabled: Boolean,
    ): Preference.PreferenceGroup {
        val getAnimeCategories = remember { Injekt.get<GetAnimeCategories>() }
        val allAnimeCategories by getAnimeCategories.subscribe().collectAsState(
            initial = runBlocking { getAnimeCategories.await() },
        )

        val discordRPCIncognitoPref = connectionsPreferences.discordRPCIncognito()
        val discordRPCIncognitoCategoriesPref = connectionsPreferences.discordRPCIncognitoCategories()

        val includedAnime by discordRPCIncognitoCategoriesPref.collectAsState()
        var showAnimeDialog by rememberSaveable { mutableStateOf(false) }
        if (showAnimeDialog) {
            TriStateListDialog(
                title = stringResource(R.string.general_categories),
                message = stringResource(R.string.pref_discord_incognito_categories_details),
                items = allAnimeCategories,
                initialChecked = includedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                initialInversed = includedAnime.mapNotNull { allAnimeCategories.find { false } },
                itemLabel = { it.visualName },
                onDismissRequest = { showAnimeDialog = false },
                onValueChanged = { newIncluded, _ ->
                    discordRPCIncognitoCategoriesPref.set(
                        newIncluded.fastMap { it.id.toString() }
                            .toSet(),
                    )
                    showAnimeDialog = false
                },
                onlyChecked = true,
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.general_categories),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = discordRPCIncognitoPref,
                    title = stringResource(R.string.pref_discord_incognito),
                    subtitle = stringResource(R.string.pref_discord_incognito_summary),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.general_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allAnimeCategories,
                        included = includedAnime,
                    ),
                    onClick = { showAnimeDialog = true },
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(R.string.pref_discord_incognito_categories_details),
                ),
            ),
            enabled = enabled,
        )
    }
}
// <-- AM (DISCORD)
