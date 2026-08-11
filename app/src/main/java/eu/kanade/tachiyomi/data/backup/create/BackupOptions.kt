package eu.kanade.tachiyomi.data.backup.create

import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR

data class BackupOptions(
    val libraryEntries: Boolean = true,
    val categories: Boolean = true,
    val chapters: Boolean = true,
    val tracking: Boolean = true,
    val history: Boolean = true,
    val readEntries: Boolean = true,
    val appSettings: Boolean = true,
    val extensionStores: Boolean = true,
    val customButton: Boolean = true,
    val sourceSettings: Boolean = true,
    val privateSettings: Boolean = false,
    val extensions: Boolean = false,
) {

    fun asBooleanArray() = booleanArrayOf(
        libraryEntries,
        categories,
        chapters,
        tracking,
        history,
        readEntries,
        appSettings,
        extensionStores,
        customButton,
        sourceSettings,
        privateSettings,
        extensions,
    )

    fun canCreate() = libraryEntries ||
        categories ||
        appSettings ||
        extensionStores ||
        customButton ||
        sourceSettings

    companion object {
        val libraryOptions = persistentListOf(
            Entry(
                label = AYMR.strings.entries,
                getter = BackupOptions::libraryEntries,
                setter = { options, enabled -> options.copy(libraryEntries = enabled) },
            ),
            Entry(
                label = AYMR.strings.chapters_episodes,
                getter = BackupOptions::chapters,
                setter = { options, enabled -> options.copy(chapters = enabled) },
                enabled = { it.libraryEntries },
            ),
            Entry(
                label = MR.strings.track,
                getter = BackupOptions::tracking,
                setter = { options, enabled -> options.copy(tracking = enabled) },
                enabled = { it.libraryEntries },
            ),
            Entry(
                label = MR.strings.history,
                getter = BackupOptions::history,
                setter = { options, enabled -> options.copy(history = enabled) },
                enabled = { it.libraryEntries },
            ),
            Entry(
                label = MR.strings.categories,
                getter = BackupOptions::categories,
                setter = { options, enabled -> options.copy(categories = enabled) },
            ),
            Entry(
                label = AYMR.strings.non_library_settings,
                getter = BackupOptions::readEntries,
                setter = { options, enabled -> options.copy(readEntries = enabled) },
                enabled = { it.libraryEntries },
            ),
        )

        val settingsOptions = persistentListOf(
            Entry(
                label = MR.strings.app_settings,
                getter = BackupOptions::appSettings,
                setter = { options, enabled -> options.copy(appSettings = enabled) },
            ),
            Entry(
                label = MR.strings.extensionStores,
                getter = BackupOptions::extensionStores,
                setter = { options, enabled -> options.copy(extensionStores = enabled) },
            ),
            Entry(
                label = AYMR.strings.custom_button_settings,
                getter = BackupOptions::customButton,
                setter = { options, enabled -> options.copy(customButton = enabled) },
            ),
            Entry(
                label = MR.strings.source_settings,
                getter = BackupOptions::sourceSettings,
                setter = { options, enabled -> options.copy(sourceSettings = enabled) },
            ),
            Entry(
                label = MR.strings.private_settings,
                getter = BackupOptions::privateSettings,
                setter = { options, enabled -> options.copy(privateSettings = enabled) },
                enabled = { it.appSettings || it.sourceSettings },
            ),
        )

        val extensionOptions = persistentListOf(
            Entry(
                label = MR.strings.label_extensions,
                getter = BackupOptions::extensions,
                setter = { options, enabled -> options.copy(extensions = enabled) },
            ),
        )

        fun fromBooleanArray(array: BooleanArray) = BackupOptions(
            libraryEntries = array[0],
            categories = array[1],
            chapters = array[2],
            tracking = array[3],
            history = array[4],
            readEntries = array[5],
            appSettings = array[6],
            extensionStores = array[7],
            customButton = array[8],
            sourceSettings = array[9],
            privateSettings = array[10],
            extensions = array[11],
        )
    }

    data class Entry(
        val label: StringResource,
        val getter: (BackupOptions) -> Boolean,
        val setter: (BackupOptions, Boolean) -> BackupOptions,
        val enabled: (BackupOptions) -> Boolean = { true },
    )
}
