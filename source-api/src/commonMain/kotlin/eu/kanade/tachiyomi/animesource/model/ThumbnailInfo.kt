package eu.kanade.tachiyomi.animesource.model

/**
 * The class containing info for displaying thumbnails.
 *
 * @param tileInfo List of tiles to be used as thumbnails
 * @param imageTileUrls List of urls to image tiles
 */
open class ThumbnailInfo(
    val tileInfo: List<TileInfo>,
    val imageTileUrls: List<String>,
)

/**
 * The class containing info for an image tile to be used as a thumbnail
 *
 * @param imageIndex The index from `imageTileUrls` to use image tile from
 * @param timeMs The position, in milliseconds, where the preview starts
 * @param x X coordinate of tile start, in pixels
 * @param y Y coordinate of tile start, in pixels
 * @param width Tile width, in pixels
 * @param height Tile height, in pixels
 */
data class TileInfo(
    val imageIndex: Int,
    val timeMs: Long,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
