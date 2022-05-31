DROP VIEW IF EXISTS animehistoryView;

/**
 *  [animehistory.episode_number] had the wrong name.
 */
CREATE VIEW animehistoryView AS
SELECT
    animehistory._id AS id,
    animes._id AS animeId,
    episodes._id AS episodeId,
    animes.title,
    animes.thumbnail_url AS thumbnailUrl,
    episodes.episode_number AS episodeNumber,
    animehistory.last_seen AS seenAt,
    max_last_seen.last_seen AS maxSeenAt,
    max_last_seen.episode_id AS maxSeenAtEpisodeId
FROM animes
JOIN episodes
ON animes._id = episodes.anime_id
JOIN animehistory
ON episodes._id = animehistory.episode_id
JOIN (
    SELECT episodes.anime_id,episodes._id AS episode_id, MAX(animehistory.last_seen) AS last_seen
    FROM episodes JOIN animehistory
    ON episodes._id = animehistory.episode_id
    GROUP BY episodes.anime_id
) AS max_last_seen
ON episodes.anime_id = max_last_seen.anime_id;