package eu.kanade.presentation.history.anime

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import java.time.Instant
import java.util.Date

object AnimeHistoryUiModelProviders {

    class HeadNow : PreviewParameterProvider<AnimeHistoryUiModel> {
        override val values: Sequence<AnimeHistoryUiModel> =
            sequenceOf(AnimeHistoryUiModel.Header(Date.from(Instant.now())))
    }
}
