package eu.kanade.presentation.history.manga

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import java.time.Instant
import java.util.Date

object MangaHistoryUiModelProviders {

    class HeadNow : PreviewParameterProvider<MangaHistoryUiModel> {
        override val values: Sequence<MangaHistoryUiModel> =
            sequenceOf(MangaHistoryUiModel.Header(Date.from(Instant.now())))
    }
}
