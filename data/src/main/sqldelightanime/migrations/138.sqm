CREATE TABLE anime_relations(
    anime_id INTEGER NOT NULL,
    related_anime_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    last_fetched_at INTEGER NOT NULL,
    PRIMARY KEY(anime_id, related_anime_id, name),
    FOREIGN KEY(anime_id) REFERENCES animes (_id)
    ON DELETE CASCADE,
    FOREIGN KEY(related_anime_id) REFERENCES animes (_id)
    ON DELETE CASCADE
);

CREATE INDEX anime_relations_anime_id_index ON anime_relations(anime_id);
CREATE INDEX anime_relations_related_anime_id_index ON anime_relations(related_anime_id);
