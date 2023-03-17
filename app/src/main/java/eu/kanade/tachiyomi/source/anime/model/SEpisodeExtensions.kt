package eu.kanade.tachiyomi.source.anime.model

import dataanime.Episodes
import eu.kanade.tachiyomi.animesource.model.SEpisode

fun SEpisode.copyFrom(other: Episodes) {
    name = other.name
    url = other.url
    date_upload = other.date_upload
    episode_number = other.episode_number
    scanlator = other.scanlator
}
