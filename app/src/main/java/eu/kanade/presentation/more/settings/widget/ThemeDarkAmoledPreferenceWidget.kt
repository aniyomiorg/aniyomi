package eu.kanade.presentation.more.settings.widget

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.supportsAmoled
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ThemeDarkAmoledPreferenceWidget(
    value: Boolean,
    themeMode: ThemeMode,
    onValueChange: (Boolean) -> Unit,
) {
    BasePreferenceWidget(
        title = stringResource(MR.strings.pref_dark_theme_pure_black),
        onClick = {
            if (themeMode.supportsAmoled()) {
                onValueChange(!value)
            }
        },
        widget = {
            Switch(
                checked = value,
                enabled = themeMode.supportsAmoled(),
                onCheckedChange = onValueChange,
            )
        },
    )
}
