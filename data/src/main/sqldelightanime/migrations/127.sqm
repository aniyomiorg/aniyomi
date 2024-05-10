-- Animes table
ALTER TABLE animes ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE animes ADD COLUMN is_syncing INTEGER NOT NULL DEFAULT 0;

-- Episodes table
ALTER TABLE episodes ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE episodes ADD COLUMN is_syncing INTEGER NOT NULL DEFAULT 0;

-- Animes triggers
DROP TRIGGER IF EXISTS update_anime_version;
CREATE TRIGGER update_anime_version AFTER UPDATE ON animes
BEGIN
    UPDATE animes SET version = version + 1
    WHERE _id = new._id AND new.is_syncing = 0 AND (
        new.url != old.url OR
        new.description != old.description OR
        new.favorite != old.favorite
    );
END;

-- Episodes triggers
DROP TRIGGER IF EXISTS update_episode_and_anime_version;
CREATE TRIGGER update_episode_and_anime_version AFTER UPDATE ON episodes
WHEN new.is_syncing = 0 AND (
    new.seen != old.seen OR
    new.bookmark != old.bookmark OR
    new.last_second_seen != old.last_second_seen
)
BEGIN
    -- Update the episode version
    UPDATE episodes SET version = version + 1
    WHERE _id = new._id;

    -- Update the anime version
    UPDATE animes SET version = version + 1
    WHERE _id = new.anime_id AND (SELECT is_syncing FROM animes WHERE _id = new.anime_id) = 0;
END;

-- anime_categories table
DROP TRIGGER IF EXISTS insert_anime_category_update_version;
CREATE TRIGGER insert_anime_category_update_version AFTER INSERT ON animes_categories
BEGIN
    UPDATE animes
    SET version = version + 1
    WHERE _id = new.anime_id AND (SELECT is_syncing FROM animes WHERE _id = new.anime_id) = 0;
END;