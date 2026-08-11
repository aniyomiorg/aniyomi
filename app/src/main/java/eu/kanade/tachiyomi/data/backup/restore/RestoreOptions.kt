package eu.kanade.tachiyomi.data.backup.restore

import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR

data class RestoreOptions(
    val libraryEntries: Boolean = true,
    val categories: Boolean = true,
    val appSettings: Boolean = true,
    val extensionStores: Boolean = true,
    val customButtons: Boolean = true,
    val sourceSettings: Boolean = true,
    val extensions: Boolean = false,
) {

    fun asBooleanArray() = booleanArrayOf(
        libraryEntries,
        categories,
        appSettings,
        extensionStores,
        customButtons,
        sourceSettings,
        extensions,
    )

    fun canRestore() = libraryEntries ||
        categories ||
        appSettings ||
        extensionStores ||
        customButtons ||
        sourceSettings ||
        extensions

    companion object {
        val options = persistentListOf(
            Entry(
                label = MR.strings.label_library,
                getter = RestoreOptions::libraryEntries,
                setter = { options, enabled -> options.copy(libraryEntries = enabled) },
            ),
            Entry(
                label = MR.strings.categories,
                getter = RestoreOptions::categories,
                setter = { options, enabled -> options.copy(categories = enabled) },
            ),
            Entry(
                label = MR.strings.app_settings,
                getter = RestoreOptions::appSettings,
                setter = { options, enabled -> options.copy(appSettings = enabled) },
            ),
            Entry(
                label = MR.strings.extensionStores,
                getter = RestoreOptions::extensionStores,
                setter = { options, enabled -> options.copy(extensionStores = enabled) },
            ),
            Entry(
                label = AYMR.strings.custom_button_settings,
                getter = RestoreOptions::customButtons,
                setter = { options, enabled -> options.copy(customButtons = enabled) },
            ),
            Entry(
                label = MR.strings.source_settings,
                getter = RestoreOptions::sourceSettings,
                setter = { options, enabled -> options.copy(sourceSettings = enabled) },
            ),
            Entry(
                label = MR.strings.label_extensions,
                getter = RestoreOptions::extensions,
                setter = { options, enabled -> options.copy(extensions = enabled) },
            ),
        )

        fun fromBooleanArray(array: BooleanArray) = RestoreOptions(
            libraryEntries = array[0],
            categories = array[1],
            appSettings = array[2],
            extensionStores = array[3],
            customButtons = array[4],
            sourceSettings = array[5],
            extensions = array[6],
        )
    }

    data class Entry(
        val label: StringResource,
        val getter: (RestoreOptions) -> Boolean,
        val setter: (RestoreOptions, Boolean) -> RestoreOptions,
    )
}
