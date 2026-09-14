package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TvUiMode
import eu.kanade.presentation.util.isTvUi
import eu.kanade.tachiyomi.util.system.isTvUiMode
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.tvFocusable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class DeviceModeStep : OnboardingStep {

    private val tvUiModePref = Injekt.get<UiPreferences>().tvUiMode()

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val detectedTv = LocalConfiguration.current.isTvUiMode()
        var selected by rememberSaveable {
            mutableStateOf(if (detectedTv) TvUiMode.ALWAYS else TvUiMode.NEVER)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Text(stringResource(AYMR.strings.onboarding_device_mode_info))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val mobileInteractionSource = remember { MutableInteractionSource() }
                SegmentedButton(
                    selected = selected == TvUiMode.NEVER,
                    onClick = {
                        selected = TvUiMode.NEVER
                        tvUiModePref.set(TvUiMode.NEVER)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    interactionSource = mobileInteractionSource,
                    modifier = Modifier.tvFocusable(mobileInteractionSource, isTvUi()),
                ) {
                    Text(stringResource(AYMR.strings.onboarding_device_mode_action_mobile))
                }
                val tvInteractionSource = remember { MutableInteractionSource() }
                SegmentedButton(
                    selected = selected == TvUiMode.ALWAYS,
                    onClick = {
                        selected = TvUiMode.ALWAYS
                        tvUiModePref.set(TvUiMode.ALWAYS)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    interactionSource = tvInteractionSource,
                    modifier = Modifier.tvFocusable(tvInteractionSource, isTvUi()),
                ) {
                    Text(stringResource(AYMR.strings.onboarding_device_mode_action_tv))
                }
            }
        }
    }
}
