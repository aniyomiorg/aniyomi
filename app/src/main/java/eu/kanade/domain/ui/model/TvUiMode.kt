package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class TvUiMode(val titleRes: StringResource) {
    AUTOMATIC(MR.strings.automatic_background),
    ALWAYS(MR.strings.lock_always),
    NEVER(MR.strings.lock_never),
}
