package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.manga.toDomainChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import tachiyomi.domain.items.service.calculateChapterGap as domainCalculateChapterGap

fun calculateChapterGap(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Int {
    return domainCalculateChapterGap(
        higherReaderChapter?.chapter?.toDomainChapter(),
        lowerReaderChapter?.chapter?.toDomainChapter(),
    )
}
