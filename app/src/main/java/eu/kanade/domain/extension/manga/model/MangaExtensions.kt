package eu.kanade.domain.extension.manga.model

import eu.kanade.tachiyomi.extension.model.Extension

data class MangaExtensions(
    val updates: List<Extension.Installed>,
    val installed: List<Extension.Installed>,
    val available: List<Extension.Available>,
    val untrusted: List<Extension.Untrusted>,
)
