package eu.kanade.presentation.more.settings.screen.player.editor.codeeditor

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.FileOutputStream

class CodeEditScreenModel(
    private val context: Context,
    private val filePath: String,
    private val storageManager: StorageManager = Injekt.get(),
) : StateScreenModel<CodeEditScreenState>(CodeEditScreenState.Loading) {
    private val _hasModified = MutableStateFlow(false)
    val hasModified = _hasModified.asStateFlow()

    private val _dialogShown = MutableStateFlow<CodeEditDialogs?>(null)
    val dialogShown = _dialogShown.asStateFlow()

    private val currentFile = MutableStateFlow<UniFile?>(null)

    init {
        screenModelScope.launchIO {
            try {
                val file = storageManager.getMPVConfigDirectory()?.findFile(filePath)
                    ?: throw Exception("Unable to read file")

                currentFile.update { _ -> file }

                val content = file.openInputStream().use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                mutableState.update { _ ->
                    CodeEditScreenState.Success(
                        TextFieldValue(
                            annotatedString = content.highlightText(),
                            composition = null,
                        ),
                    )
                }
            } catch (e: Exception) {
                mutableState.update { _ -> CodeEditScreenState.Error(e) }
            }
        }
    }

    private val luaHighlight = luaHighlight(githubTheme)
    private val confHighlight = confHighlight(githubTheme)
    private fun String.highlightText(): AnnotatedString {
        return if (this.length > HIGHLIGHT_MAX_SIZE) {
            AnnotatedString(this)
        } else {
            when (filePath.substringAfterLast(".")) {
                "lua" -> luaHighlight.toAnnotatedString(this)
                "conf" -> confHighlight.toAnnotatedString(this)
                else -> AnnotatedString(this)
            }
        }
    }

    fun showDialog(dialog: CodeEditDialogs) {
        _dialogShown.update { _ -> dialog }
    }

    fun dismissDialog() {
        _dialogShown.update { _ -> null }
    }

    fun onEdit(value: TextFieldValue) {
        mutableState.update { current ->
            if (value.text != (current as? CodeEditScreenState.Success)?.content?.text) {
                _hasModified.update { _ -> true }

                CodeEditScreenState.Success(
                    TextFieldValue(
                        annotatedString = value.text.highlightText(),
                        selection = value.selection,
                        composition = null,
                    ),
                )
            } else {
                CodeEditScreenState.Success(
                    TextFieldValue(
                        annotatedString = current.content.annotatedString,
                        selection = value.selection,
                        composition = null,
                    ),
                )
            }
        }
    }

    fun save() {
        val file = currentFile.value ?: kotlin.run {
            context.toast(AYMR.strings.editor_save_error)
            return
        }

        val content = (mutableState.value as? CodeEditScreenState.Success)
            ?.content?.annotatedString?.text ?: kotlin.run {
            context.toast(AYMR.strings.editor_save_error)
            return
        }

        try {
            file.openOutputStream()
                .also { (it as? FileOutputStream)?.channel?.truncate(0) }
                .use { it.write(content.toByteArray()) }
            _hasModified.update { _ -> false }
            context.toast(context.stringResource(AYMR.strings.editor_save_success))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            context.toast(e.message ?: context.stringResource(AYMR.strings.editor_save_error))
        }
    }

    companion object {
        private const val HIGHLIGHT_MAX_SIZE = 15000
    }
}

sealed interface CodeEditDialogs {
    data object GoBack : CodeEditDialogs
}

sealed interface CodeEditScreenState {
    @Immutable
    data object Loading : CodeEditScreenState

    @Immutable
    data class Success(
        val content: TextFieldValue,
    ) : CodeEditScreenState

    @Immutable
    data class Error(
        val throwable: Throwable,
    ) : CodeEditScreenState
}
