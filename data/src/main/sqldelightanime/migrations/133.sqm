DROP INDEX IF EXISTS animes_parent_id;
DROP INDEX IF EXISTS animes_fetch_type;
CREATE INDEX animes_parent_id ON animes(parent_id);
CREATE INDEX animes_fetch_type ON animes(fetch_type);

DROP VIEW IF EXISTS animeseasonstatsView;
CREATE VIEW animeseasonstatsView AS
SELECT
    season.parent_id,
    count(*) AS child_count,
    sum(CASE WHEN season.fetch_type = 1 AND
                 coalesce(ES.total, 0) = coalesce(ES.seenCount, 0)
            THEN 1 ELSE 0 END) AS fully_seen_seasons,
    max(coalesce(ES.latestUpload, 0)) AS max_latest_upload,
    max(coalesce(ES.fetchedAt, 0)) AS max_fetched_at,
    max(coalesce(AHS.lastSeen, 0)) AS max_last_seen,
    sum(coalesce(ES.bookmarkCount, 0)) AS total_bookmarks
FROM animes season
LEFT JOIN episodestatsView ES ON season._id = ES.anime_id
LEFT JOIN animehistorystatsView AHS ON season._id = AHS.anime_id
WHERE season.parent_id IS NOT NULL
GROUP BY season.parent_id;

DROP VIEW IF EXISTS animelibView;
CREATE VIEW animelibView AS
SELECT
    M.*,
    CASE M.fetch_type
        WHEN 1 THEN coalesce(ES.total, 0)
        WHEN 0 THEN coalesce(ASS.child_count, 0)
    END AS totalCount,
    CASE M.fetch_type
        WHEN 1 THEN coalesce(ES.seenCount, 0)
        WHEN 0 THEN coalesce(ASS.fully_seen_seasons, 0)
    END AS seenCount,
    CASE M.fetch_type
        WHEN 1 THEN coalesce(ES.latestUpload, 0)
        WHEN 0 THEN coalesce(ASS.max_latest_upload, 0)
    END AS latestUpload,
    CASE M.fetch_type
        WHEN 1 THEN coalesce(ES.fetchedAt, 0)
        WHEN 0 THEN coalesce(ASS.max_fetched_at, 0)
    END AS episodeFetchedAt,
    CASE M.fetch_type
        WHEN 1 THEN coalesce(AHS.lastSeen, 0)
        WHEN 0 THEN coalesce(ASS.max_last_seen, 0)
    END AS lastSeen,
    CASE M.fetch_type
        WHEN 1 THEN coalesce(ES.bookmarkCount, 0)
        WHEN 0 THEN coalesce(ASS.total_bookmarks, 0)
    END AS bookmarkCount,
    coalesce(MC.category_id, 0) AS category
FROM animes M
LEFT JOIN episodestatsView AS ES ON M._id = ES.anime_id
LEFT JOIN animehistorystatsView AS AHS ON M._id = AHS.anime_id
LEFT JOIN animes_categories AS MC ON MC.anime_id = M._id
LEFT JOIN animeseasonstatsView AS ASS ON M._id = ASS.parent_id
WHERE M.favorite = 1;
