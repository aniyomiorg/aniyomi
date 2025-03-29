package eu.kanade.presentation.more.settings

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.core.preference.asState
import eu.kanade.presentation.more.settings.Preference.PreferenceItem
import eu.kanade.tachiyomi.data.track.Tracker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.CoroutineScope
import mihon.core.archive.openFileDescriptor
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.FileOutputStream
import tachiyomi.core.common.preference.Preference as PreferenceData

sealed class Preference {
    abstract val title: String
    abstract val enabled: Boolean

    sealed class PreferenceItem<T> : Preference() {
        abstract val subtitle: String?
        abstract val icon: ImageVector?
        abstract val onValueChanged: suspend (newValue: T) -> Boolean

        /**
         * A basic [PreferenceItem] that only displays texts.
         */
        data class TextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },

            val onClick: (() -> Unit)? = null,
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that provides a two-state toggleable option.
         */
        data class SwitchPreference(
            val pref: PreferenceData<Boolean>,
            override val title: String,
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Boolean) -> Boolean = { true },
        ) : PreferenceItem<Boolean>()

        /**
         * A [PreferenceItem] that provides a slider to select an integer number.
         */
        data class SliderPreference(
            val value: Int,
            val min: Int = 0,
            val max: Int,
            override val title: String = "",
            override val subtitle: String? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Int) -> Boolean = { true },
        ) : PreferenceItem<Int>()

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         */
        @Suppress("UNCHECKED_CAST")
        data class ListPreference<T>(
            val pref: PreferenceData<T>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: T, entries: ImmutableMap<T, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: T) -> Boolean = { true },

            val entries: ImmutableMap<T, String>,
        ) : PreferenceItem<T>() {
            internal fun internalSet(newValue: Any) = pref.set(newValue as T)
            internal suspend fun internalOnValueChanged(newValue: Any) = onValueChanged(
                newValue as T,
            )

            @Composable
            internal fun internalSubtitleProvider(value: Any?, entries: ImmutableMap<out Any?, String>) =
                subtitleProvider(value as T, entries as ImmutableMap<T, String>)
        }

        /**
         * [ListPreference] but with no connection to a [PreferenceData]
         */
        data class BasicListPreference(
            val value: String,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: String, entries: ImmutableMap<String, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },

            val entries: ImmutableMap<String, String>,
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         * Multiple entries can be selected at the same time.
         */
        data class MultiSelectListPreference(
            val pref: PreferenceData<Set<String>>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (
                value: Set<String>,
                entries: ImmutableMap<String, String>,
            ) -> String? = { v, e ->
                val combined = remember(v) {
                    v.map { e[it] }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString()
                } ?: stringResource(MR.strings.none)
                subtitle?.format(combined)
            },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Set<String>) -> Boolean = { true },

            val entries: ImmutableMap<String, String>,
        ) : PreferenceItem<Set<String>>()

        /**
         * A [PreferenceItem] that shows a EditText in the dialog.
         */
        data class EditTextPreference(
            val pref: PreferenceData<String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that shows a multi-line EditText in the dialog.
         */
        data class MultiLineEditTextPreference(
            val pref: PreferenceData<String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
            val canBeBlank: Boolean = false,
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] for editing MPV config files.
         * If [fileName] is not null, it will update this file in the config directory.
         */
        data class MPVConfPreference(
            val pref: PreferenceData<String>,
            val scope: CoroutineScope,
            val context: Context,
            val fileName: String? = null,
            override val title: String,
            override val subtitle: String? = pref.asState(scope).value
                .lines().take(2)
                .joinToString(
                    separator = "\n",
                    postfix = if (pref.asState(scope).value.lines().size > 2) "\n..." else "",
                ),
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { newValue ->
                if (fileName != null) {
                    val storageManager: StorageManager = Injekt.get()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                        val inputFile = storageManager.getMPVConfigDirectory()
                            ?.createFile(fileName)
                        inputFile?.openFileDescriptor(context, "rwt")?.fileDescriptor
                            ?.let {
                                FileOutputStream(it).bufferedWriter().use { writer ->
                                    writer.write(newValue)
                                }
                            }
                        pref.set(newValue)
                    }
                }
                true
            },
            val canBeBlank: Boolean = true,
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that shows a EditText with a subtitle in the dialog.
         * Unlike [EditTextPreference], empty values can be set and a subtitle in the dialog can be show.
         */
        data class EditTextInfoPreference(
            val pref: PreferenceData<String>,
            val dialogSubtitle: String?,
            override val title: String,
            override val subtitle: String? = "%s",
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] for individual tracker.
         */
        data class TrackerPreference(
            val tracker: Tracker,
            override val title: String,
            val login: () -> Unit,
            val logout: () -> Unit,
        ) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true }
        }

        data class InfoPreference(
            override val title: String,
            override val enabled: Boolean = true,
        ) : PreferenceItem<String>() {
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true }
        }

        data class CustomPreference(
            override val title: String,
            val content: @Composable (PreferenceItem<String>) -> Unit,
        ) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true }
        }
    }

    data class PreferenceGroup(
        override val title: String,
        override val enabled: Boolean = true,

        val preferenceItems: ImmutableList<PreferenceItem<out Any>>,
    ) : Preference()
}
