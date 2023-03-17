package eu.kanade.tachiyomi.source.manga.model

import data.Chapters
import eu.kanade.tachiyomi.source.model.SChapter

fun SChapter.copyFrom(other: Chapters) {
    name = other.name
    url = other.url
    date_upload = other.date_upload
    chapter_number = other.chapter_number
    scanlator = other.scanlator
}
