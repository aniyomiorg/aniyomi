package eu.kanade.domain.entries.episode.interactor

import eu.kanade.domain.entries.episode.model.Episode

class ShouldUpdateDbEpisode {

    fun await(dbEpisode: Episode, sourceEpisode: Episode): Boolean {
        return dbEpisode.scanlator != sourceEpisode.scanlator || dbEpisode.name != sourceEpisode.name ||
            dbEpisode.dateUpload != sourceEpisode.dateUpload ||
            dbEpisode.episodeNumber != sourceEpisode.episodeNumber ||
            dbEpisode.sourceOrder != sourceEpisode.sourceOrder
    }
}
