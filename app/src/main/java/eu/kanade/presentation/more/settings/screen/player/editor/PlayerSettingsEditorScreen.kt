package eu.kanade.presentation.more.settings.screen.player.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.player.editor.codeeditor.CodeEditScreen
import eu.kanade.presentation.more.settings.screen.player.editor.components.EditorScreen
import eu.kanade.presentation.more.settings.screen.player.editor.components.FileCreateDialog
import eu.kanade.presentation.more.settings.screen.player.editor.components.FileDeleteDialog
import eu.kanade.presentation.util.Screen

object PlayerSettingsEditorScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { PlayerSettingsEditorScreenModel(context) }

        val state by screenModel.state.collectAsState()
        val dialog by screenModel.dialogShown.collectAsState()
        val selectedType by screenModel.selectedType.collectAsState()

        when (dialog) {
            null -> {}
            EditorFileDialog.Create -> {
                FileCreateDialog(
                    initialName = null,
                    fileExtension = selectedType.fileExtension,
                    onDismissRequest = screenModel::dismissDialog,
                    isValid = screenModel::isValidName,
                    onConfirm = screenModel::createFile,
                )
            }
            is EditorFileDialog.Edit -> {
                val name = (dialog as EditorFileDialog.Edit).item.name
                FileCreateDialog(
                    initialName = name,
                    fileExtension = selectedType.fileExtension,
                    onDismissRequest = screenModel::dismissDialog,
                    isValid = screenModel::isValidName,
                    onConfirm = { screenModel.editFile(name, it) },
                )
            }
            is EditorFileDialog.Delete -> {
                val name = (dialog as EditorFileDialog.Delete).item.name
                FileDeleteDialog(
                    name = name,
                    onDismissRequest = screenModel::dismissDialog,
                    onDelete = { screenModel.deleteFile(name) },
                )
            }
        }

        EditorScreen(
            state = state,
            selectedType = selectedType,
            onSelectType = screenModel::onSelectType,
            onClickItem = {
                screenModel.getFilePath(it.name).let { filePath ->
                    navigator.push(CodeEditScreen(filePath))
                }
            },
            onRenameItem = { screenModel.showDialog(EditorFileDialog.Edit(it)) },
            onDeleteItem = { screenModel.showDialog(EditorFileDialog.Delete(it)) },
            onClickAdd = { screenModel.showDialog(EditorFileDialog.Create) },
            navigateUp = navigator::pop,
        )
    }
}
