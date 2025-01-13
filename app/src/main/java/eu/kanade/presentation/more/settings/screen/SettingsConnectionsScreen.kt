// AM (CONNECTIONS) -->
package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.connections.ConnectionsService
import eu.kanade.tachiyomi.util.system.openDiscordLoginActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsConnectionsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    @StringRes
    override fun getTitleRes() = TLMR.strings.pref_category_connections

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val connectionsManager = remember { Injekt.get<ConnectionsManager>() }

        var dialog by remember { mutableStateOf<Any?>(null) }
        dialog?.run {
            when (this) {
                is LoginConnectionsDialog -> {
                    ConnectionsLoginDialog(
                        service = service,
                        uNameStringRes = uNameStringRes,
                        onDismissRequest = { dialog = null },
                    )
                }
            }
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.special_services),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ConnectionsPreference(
                        title = stringResource(connectionsManager.discord.nameRes()),
                        service = connectionsManager.discord,
                        login = {
                            context.openDiscordLoginActivity()
                        },
                        openSettings = { navigator.push(SettingsDiscordScreen) },
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(R.string.connections_discord_info),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(R.string.connections_info),
                    ),
                ),
            ),
        )
    }

    @Composable
    @Suppress("LongMethod")
    private fun ConnectionsLoginDialog(
        service: ConnectionsService,
        @StringRes uNameStringRes: Int,
        onDismissRequest: () -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var username by remember { mutableStateOf(TextFieldValue(service.getUsername())) }
        var password by remember { mutableStateOf(TextFieldValue(service.getPassword())) }
        var processing by remember { mutableStateOf(false) }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.login_title,
                            stringResource(service.nameRes()),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(text = stringResource(uNameStringRes)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        isError = inputError && username.text.isEmpty(),
                    )

                    var hidePassword by remember { mutableStateOf(true) }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(text = stringResource(R.string.password)) },
                        trailingIcon = {
                            IconButton(onClick = { hidePassword = !hidePassword }) {
                                Icon(
                                    imageVector = if (hidePassword) {
                                        Icons.Filled.Visibility
                                    } else {
                                        Icons.Filled.VisibilityOff
                                    },
                                    contentDescription = null,
                                )
                            }
                        },
                        visualTransformation = if (hidePassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        singleLine = true,
                        isError = inputError && password.text.isEmpty(),
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !processing,
                    onClick = {
                        if (username.text.isEmpty() || password.text.isEmpty()) {
                            inputError = true
                            return@Button
                        }
                        scope.launchIO {
                            inputError = false
                            processing = true
                            val result = checkLogin(
                                context = context,
                                service = service,
                                username = username.text,
                                password = password.text,
                            )
                            if (result) onDismissRequest()
                            processing = false
                        }
                    },
                ) {
                    val id = if (processing) R.string.loading else R.string.login
                    Text(text = stringResource(id))
                }
            },
        )
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private suspend fun checkLogin(
        context: Context,
        service: ConnectionsService,
        username: String,
        password: String,
    ): Boolean {
        return try {
            service.login(username, password)
            withUIContext { context.toast(MR.strings.login_success) }
            true
        } catch (e: Throwable) {
            service.logout()
            withUIContext { context.toast(e.message.toString()) }
            false
        }
    }
}

@Composable
internal fun ConnectionsLogoutDialog(
    service: ConnectionsService,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.logout_title, stringResource(service.nameRes())),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest,
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        service.logout()
                        onDismissRequest()
                        context.toast(MR.strings.logout_success)
                        navigator.pop()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.logout))
                }
            }
        },
    )
}

@Suppress("UnusedPrivateClass")
private data class LoginConnectionsDialog(
    val service: ConnectionsService,
    @StringRes val uNameStringRes: Int,
)

internal data class LogoutConnectionsDialog(
    val service: ConnectionsService,
)
// <-- AM (CONNECTIONS)
