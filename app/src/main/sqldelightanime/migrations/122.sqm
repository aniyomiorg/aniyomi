DROP VIEW IF EXISTS animelibView;

CREATE VIEW animelibView AS
SELECT
    A.*,
    coalesce(E.total, 0) AS totalCount,
    coalesce(E.seenCount, 0) AS seenCount,
    coalesce(E.latestUpload, 0) AS latestUpload,
    coalesce(E.fetchedAt, 0) AS episodeFetchedAt,
    coalesce(E.lastSeen, 0) AS lastSeen,
    coalesce(E.bookmarkCount, 0) AS bookmarkCount,
    coalesce(AC.category_id, 0) AS category
FROM animes A
LEFT JOIN(
    SELECT
        episodes.anime_id,
        count(*) AS total,
        sum(seen) AS seenCount,
        coalesce(max(episodes.date_upload), 0) AS latestUpload,
        coalesce(max(animehistory.last_seen), 0) AS lastSeen,
        coalesce(max(episodes.date_fetch), 0) AS fetchedAt,
        sum(episodes.bookmark) AS bookmarkCount
    FROM episodes
    LEFT JOIN animehistory
    ON episodes._id = animehistory.episode_id
    GROUP BY episodes.anime_id
) AS E
ON A._id = E.anime_id
LEFT JOIN animes_categories AS AC
ON AC.anime_id = A._id
WHERE A.favorite = 1;