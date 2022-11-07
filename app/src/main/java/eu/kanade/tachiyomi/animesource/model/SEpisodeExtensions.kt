package eu.kanade.tachiyomi.animesource.model

import dataanime.Episodes

fun SEpisode.copyFrom(other: Episodes) {
    name = other.name
    url = other.url
    date_upload = other.date_upload
    episode_number = other.episode_number
    scanlator = other.scanlator
}
