package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.player.Decoder
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DecodersSheet(
    selectedDecoder: Decoder,
    onSelect: (Decoder) -> Unit,
    onDismissRequest: () -> Unit,
) {
    GenericTracksSheet(
        Decoder.entries.minusElement(Decoder.Auto).toImmutableList(),
        track = {
            AudioTrackRow(
                title = stringResource(MR.strings.player_sheets_decoder_formatted, it.title, it.value),
                isSelected = selectedDecoder == it,
                onClick = { onSelect(it) },
            )
        },
        onDismissRequest = onDismissRequest,
    )
}
