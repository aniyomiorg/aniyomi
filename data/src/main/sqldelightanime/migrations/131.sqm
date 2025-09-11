-- Add fetch type column, all current entries are episodes
ALTER TABLE animes ADD COLUMN fetch_type INTEGER NOT NULL DEFAULT 1;

-- Add parent id column
ALTER TABLE animes ADD COLUMN parent_id INTEGER;

-- Add season flags column
ALTER TABLE animes ADD COLUMN season_flags INTEGER NOT NULL DEFAULT 0;

-- Add season number column
ALTER TABLE animes ADD COLUMN season_number REAL NOT NULL DEFAULT -1.0;

-- Add source order for seasons
ALTER TABLE animes ADD COLUMN season_source_order INTEGER NOT NULL DEFAULT 0;

-- Create views
DROP VIEW IF EXISTS episodestatsView;
CREATE VIEW episodestatsView AS
SELECT
    e.anime_id,
    count(*) AS total,
    sum(seen) AS seenCount,
    coalesce(max(e.date_upload), 0) AS latestUpload,
    coalesce(max(e.date_fetch), 0) AS fetchedAt,
    sum(e.bookmark) AS bookmarkCount
FROM episodes e
GROUP BY e.anime_id;

DROP VIEW IF EXISTS animehistorystatsView;
CREATE VIEW animehistorystatsView AS
SELECT
    e.anime_id,
    max(eh.last_seen) AS lastSeen
FROM episodes e
LEFT JOIN animehistory AS eh ON e._id = eh.episode_id
GROUP BY e.anime_id;

DROP VIEW IF EXISTS animelibView;
CREATE VIEW animelibView AS
SELECT
    A.*,
    CASE A.fetch_type
        WHEN 1 THEN coalesce(ESA.total, 0)
        WHEN 0 THEN (SELECT count(*) FROM animes WHERE parent_id = A._id)
    END AS totalCount,
    CASE A.fetch_type
        WHEN 1 THEN coalesce(ESA.seenCount, 0)
        WHEN 0 THEN (
            SELECT count(*)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = A._id
              AND season.fetch_type = 1
              AND coalesce(season_stats.total, 0) = coalesce(season_stats.seenCount, 0)
        )
    END AS seenCount,
    CASE A.fetch_type
        WHEN 1 THEN coalesce(ESA.latestUpload, 0)
        WHEN 0 THEN (
            SELECT coalesce(max(season_stats.latestUpload), 0)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = A._id
        )
    END AS latestUpload,
    CASE A.fetch_type
        WHEN 1 THEN coalesce(ESA.fetchedAt, 0)
        WHEN 0 THEN (
            SELECT coalesce(max(season_stats.fetchedAt), 0)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = A._id
        )
    END AS episodeFetchedAt,
    CASE A.fetch_type
        WHEN 1 THEN coalesce(AHSA.lastSeen, 0)
        WHEN 0 THEN (
            SELECT coalesce(max(child_history.lastSeen), 0)
            FROM animes season
            LEFT JOIN animehistorystatsView child_history ON season._id = child_history.anime_id
            WHERE season.parent_id = A._id
        )
    END AS lastSeen,
    CASE A.fetch_type
        WHEN 1 THEN coalesce(ESA.bookmarkCount, 0)
        WHEN 0 THEN (
            SELECT coalesce(sum(season_stats.bookmarkCount), 0)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = A._id
        )
        ELSE 0
    END AS bookmarkCount,
    coalesce(MCA.category_id, 0) AS category
FROM animes A
LEFT JOIN episodestatsView AS ESA
ON A._id = ESA.anime_id
LEFT JOIN animehistorystatsView AS AHSA
ON A._id = AHSA.anime_id
LEFT JOIN animes_categories AS MCA
ON MCA.anime_id = A._id
WHERE A.favorite = 1;

DROP VIEW IF EXISTS animeseasonsView;
CREATE VIEW animeseasonsView AS
SELECT
    SA.*,
    CASE SA.fetch_type
        WHEN 1 THEN coalesce(ESSA.total, 0)
        WHEN 0 THEN (SELECT count(*) FROM animes WHERE parent_id = SA._id)
    END AS totalCount,
    CASE SA.fetch_type
        WHEN 1 THEN coalesce(ESSA.seenCount, 0)
        WHEN 0 THEN (
            SELECT count(*)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = SA._id
              AND season.fetch_type = 1
              AND coalesce(season_stats.total, 0) = coalesce(season_stats.seenCount, 0)
        )
    END AS seenCount,
    CASE SA.fetch_type
        WHEN 1 THEN coalesce(ESSA.latestUpload, 0)
        WHEN 0 THEN (
            SELECT coalesce(max(season_stats.latestUpload), 0)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = SA._id
        )
    END AS latestUpload,
    CASE SA.fetch_type
        WHEN 1 THEN coalesce(ESSA.fetchedAt, 0)
        WHEN 0 THEN (
            SELECT coalesce(max(season_stats.fetchedAt), 0)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = SA._id
        )
    END AS episodeFetchedAt,
    CASE SA.fetch_type
        WHEN 1 THEN coalesce(AHSSA.lastSeen, 0)
        WHEN 0 THEN (
            SELECT coalesce(max(child_history.lastSeen), 0)
            FROM animes season
            LEFT JOIN animehistorystatsView child_history ON season._id = child_history.anime_id
            WHERE season.parent_id = SA._id
        )
    END AS lastSeen,
    CASE SA.fetch_type
        WHEN 1 THEN coalesce(ESSA.bookmarkCount, 0)
        WHEN 0 THEN (
            SELECT sum(season_stats.bookmarkCount)
            FROM animes season
            LEFT JOIN episodestatsView season_stats ON season._id = season_stats.anime_id
            WHERE season.parent_id = SA._id
        )
    END AS bookmarkCount
FROM animes AS SA
LEFT JOIN episodestatsView AS ESSA
ON SA._id = ESSA.anime_id
LEFT JOIN animehistorystatsView AS AHSSA
ON SA._id = AHSSA.anime_id
WHERE SA.parent_id IS NOT NULL;

-- Create view for getting deletable anime
DROP VIEW IF EXISTS animedeletableView;
CREATE VIEW animedeletableView AS
SELECT
    A._id,
    A.source,
    A.fetch_type
FROM animes AS A
WHERE A.favorite = 0
AND A.parent_id IS NULL;
