package eu.kanade.presentation.more.settings.screen.player.editor.codeeditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.screen.player.editor.components.UnsavedChangesDialog
import eu.kanade.presentation.util.Screen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class CodeEditScreen(private val filePath: String) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { CodeEditScreenModel(context, filePath) }

        val state by screenModel.state.collectAsState()
        val dialogShown by screenModel.dialogShown.collectAsState()
        val hasModified by screenModel.hasModified.collectAsState()

        BackHandler(enabled = hasModified) {
            screenModel.showDialog(CodeEditDialogs.GoBack)
        }

        when (dialogShown) {
            null -> {}
            CodeEditDialogs.GoBack -> {
                UnsavedChangesDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onConfirm = { navigator.pop() },
                )
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    navigateUp = {
                        if (hasModified) {
                            screenModel.showDialog(CodeEditDialogs.GoBack)
                        } else {
                            navigator.pop()
                        }
                    },
                    titleContent = {
                        Text(text = filePath.substringAfter("/"))
                    },
                    actions = {
                        AppBarActions(
                            actions = persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_save),
                                    icon = Icons.Outlined.Save,
                                    onClick = screenModel::save,
                                    enabled = hasModified,
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when (state) {
                CodeEditScreenState.Loading -> {
                    LoadingScreen()
                }
                is CodeEditScreenState.Error -> {
                    EmptyScreen(
                        message = (state as CodeEditScreenState.Error).throwable.message ?: "Unknown exception",
                    )
                }
                is CodeEditScreenState.Success -> {
                    CodeEditorContent(
                        state = state as CodeEditScreenState.Success,
                        contentPadding = contentPadding,
                        onEdit = screenModel::onEdit,
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeEditorContent(
    state: CodeEditScreenState.Success,
    contentPadding: PaddingValues,
    onEdit: (TextFieldValue) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    val focusRequester = remember { FocusRequester() }
    var lineCount by remember { mutableIntStateOf(1) }

    val codeStyle = TextStyle(
        color = MaterialTheme.colorScheme.onBackground,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    Row(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .imePadding(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        BasicTextField(
            modifier = Modifier
                .padding(
                    start = contentPadding.calculateLeftPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
                .fillMaxHeight()
                .width(12.dp * lineCount.toString().length),
            value = IntRange(1, lineCount).joinToString(separator = "\n"),
            readOnly = true,
            textStyle = codeStyle.copy(
                color = codeStyle.color.copy(alpha = DISABLED_ALPHA),
                textAlign = TextAlign.End,
            ),
            onValueChange = {},
        )

        BasicTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
            value = state.content,
            onValueChange = { onEdit(it) },
            onTextLayout = { result ->
                lineCount = result.lineCount
            },
            textStyle = codeStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}
