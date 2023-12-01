package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.core.i18n.localize
import tachiyomi.presentation.core.i18n.localize


enum class TabletUiMode(val titleRes: StringResource) {
    AUTOMATIC(MR.strings.automatic_background),
    ALWAYS(MR.strings.lock_always),
    LANDSCAPE(MR.strings.landscape),
    NEVER(MR.strings.lock_never),
}
