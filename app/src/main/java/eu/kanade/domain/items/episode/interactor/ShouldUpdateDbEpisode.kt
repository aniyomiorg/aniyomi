package eu.kanade.domain.items.episode.interactor

import eu.kanade.domain.items.episode.model.Episode

class ShouldUpdateDbEpisode {

    fun await(dbEpisode: Episode, sourceEpisode: Episode): Boolean {
        return dbEpisode.scanlator != sourceEpisode.scanlator || dbEpisode.name != sourceEpisode.name ||
            dbEpisode.dateUpload != sourceEpisode.dateUpload ||
            dbEpisode.episodeNumber != sourceEpisode.episodeNumber ||
            dbEpisode.sourceOrder != sourceEpisode.sourceOrder
    }
}
