package eu.kanade.presentation.updates.manga.model

import tachiyomi.domain.entries.manga.model.MangaCover

class MangaUpdatesUiModels {
    data class Manga(
        val mangaId: Long,
        val mangaTitle: String,
        val coverData: MangaCover,
        val subText: String,
    )
}
