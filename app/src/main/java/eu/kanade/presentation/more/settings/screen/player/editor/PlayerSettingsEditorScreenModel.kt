package eu.kanade.presentation.more.settings.screen.player.editor

import android.content.Context
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.util.size
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.toSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.storage.service.SCRIPTS_PATH
import tachiyomi.domain.storage.service.SCRIPT_OPTS_PATH
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlayerSettingsEditorScreenModel(
    private val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
) : StateScreenModel<EditorScreenState>(EditorScreenState.Loading) {
    private val _selectedType = MutableStateFlow(EditorListType.SCRIPTS)
    val selectedType = _selectedType.asStateFlow()

    private val _dialogShown = MutableStateFlow<EditorFileDialog?>(null)
    val dialogShown = _dialogShown.asStateFlow()

    init {
        screenModelScope.launchIO {
            _selectedType.collectLatest { type ->
                updateItems(type)
            }
        }
    }

    private fun updateItems(type: EditorListType) {
        mutableState.update {
            EditorScreenState.Success(
                editorListItems = getEditorListItems(type),
            )
        }
    }

    fun createFile(fileName: String) {
        storageManager.getMPVConfigDirectory()
            ?.createDirectory(selectedType.value.directoryName)
            ?.createFile(fileName)
            ?: run {
                context.toast(context.stringResource(AYMR.strings.editor_create_error))
                return
            }

        updateItems(selectedType.value)
    }

    fun editFile(originalFile: String, fileName: String) {
        val file = storageManager.getMPVConfigDirectory()
            ?.createDirectory(selectedType.value.directoryName)
            ?.createFile(originalFile)

        if (file?.renameTo(fileName) == true) {
            updateItems(selectedType.value)
        } else {
            context.toast(context.stringResource(AYMR.strings.editor_rename_error))
        }
    }

    fun deleteFile(name: String) {
        val file = storageManager.getMPVConfigDirectory()
            ?.createDirectory(selectedType.value.directoryName)
            ?.findFile(name)

        if (file?.delete() == true) {
            updateItems(selectedType.value)
        } else {
            context.toast(context.stringResource(AYMR.strings.editor_delete_error))
        }
    }

    fun isValidName(name: String, initialName: String? = null): FileCreationResult {
        val names = (mutableState.value as? EditorScreenState.Success)
            ?.editorListItems
            ?.map { it.name }
            ?.filterNot { it == initialName }
            .orEmpty()

        if (names.any { it.equals(name, true) }) {
            return FileCreationResult.Failure(AYMR.strings.editor_file_already_exists)
        }

        if (name != DiskUtil.buildValidFilename(name)) {
            return FileCreationResult.Failure(AYMR.strings.editor_invalid_filename)
        }

        return FileCreationResult.Success
    }

    fun showDialog(dialog: EditorFileDialog) {
        _dialogShown.update { _ -> dialog }
    }

    fun dismissDialog() {
        _dialogShown.update { _ -> null }
    }

    fun onSelectType(type: EditorListType) {
        _selectedType.update { _ -> type }
    }

    fun getFilePath(name: String): String {
        return "${selectedType.value.directoryName}/$name"
    }

    private fun getEditorListItems(type: EditorListType): List<EditorListItem> {
        val directory = storageManager.getMPVConfigDirectory()?.createDirectory(type.directoryName)
            ?: return emptyList()

        val dateFormat = SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.getDefault())

        return directory.listFiles()?.mapNotNull { file ->
            if (file.isDirectory) {
                return@mapNotNull null
            }

            val lastModified = file.lastModified().takeIf { it != -1L }?.let {
                dateFormat.format(Date(it))
            }

            EditorListItem(
                name = file.name ?: "",
                size = file.size().toSize(),
                lastModified = lastModified,
            )
        }.orEmpty()
    }
}

sealed interface FileCreationResult {
    data object Success : FileCreationResult
    data class Failure(val stringRes: StringResource) : FileCreationResult
}

sealed interface EditorFileDialog {
    data object Create : EditorFileDialog
    data class Edit(val item: EditorListItem) : EditorFileDialog
    data class Delete(val item: EditorListItem) : EditorFileDialog
}

enum class EditorListType(val directoryName: String, val stringRes: StringResource, val fileExtension: String) {
    SCRIPTS(SCRIPTS_PATH, AYMR.strings.pref_player_editor_script, "lua"),
    SCRIPTS_OPTS(SCRIPT_OPTS_PATH, AYMR.strings.pref_player_editor_script_opts, "conf"),
}

data class EditorListItem(
    val name: String,
    val size: String? = null,
    val lastModified: String? = null,
)

sealed interface EditorScreenState {
    @Immutable
    data object Loading : EditorScreenState

    @Immutable
    data class Success(
        val editorListItems: List<EditorListItem>,
    ) : EditorScreenState {
        val isEmpty: Boolean
            get() = editorListItems.isEmpty()
    }
}
