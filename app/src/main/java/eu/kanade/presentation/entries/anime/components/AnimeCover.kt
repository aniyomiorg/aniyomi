package eu.kanade.presentation.entries.anime.components

import androidx.annotation.ColorInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import eu.kanade.tachiyomi.R
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.domain.entries.anime.model.AnimeCover as DomainAnimeCover

enum class AnimeCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),

    // KMK -->
    Panorama(3f / 2f),
    // KMK <--
    ;

    enum class Size {
        Normal,
        Medium,
        Big,
    }

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
        // KMK -->
        alpha: Float = 1f,
        bgColor: Color? = null,
        @ColorInt tint: Int? = null,
        /** Perform action when cover loaded, specifically generating color map. If the cover doesn't update, it won't be called */
        onCoverLoaded: ((DomainAnimeCover, result: AsyncImagePainter.State.Success) -> Unit)? = null,
        size: Size = Size.Normal,
        scale: ContentScale = ContentScale.Crop,
        // KMK <--
    ) {
        // KMK -->
        var succeed by remember { mutableStateOf(false) }
        // KMK <--

        val modifierColored = modifier
            .aspectRatio(ratio)
            .clip(shape)
            // KMK -->
            .alpha(if (succeed) alpha else 1f)
            .background(bgColor ?: CoverPlaceholderColor)
            // KMK <--
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )

        SubcomposeAsyncImage(
            model = data,
            // KMK -->
            loading = {
                Box(
                    modifier = modifierColored,
                ) {
                    CircularProgressIndicator(
                        color = tint?.let { Color(it) } ?: CoverPlaceholderOnBgColor,
                        modifier = Modifier
                            .size(
                                when (size) {
                                    Size.Big -> COVER_TEMPLATE_SIZE_BIG
                                    Size.Medium -> COVER_TEMPLATE_SIZE_MEDIUM
                                    else -> COVER_TEMPLATE_SIZE_NORMAL
                                },
                            )
                            .align(Alignment.Center),
                        strokeWidth = when (size) {
                            Size.Normal -> 3.dp
                            else -> 2.dp
                        },
                    )
                }
            },
            error = {
                Box(
                    modifier = modifierColored,
                ) {
                    Image(
                        imageVector = ImageVector.vectorResource(R.drawable.cover_error_vector),
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .size(
                                when (size) {
                                    Size.Big -> COVER_TEMPLATE_SIZE_BIG
                                    Size.Medium -> COVER_TEMPLATE_SIZE_MEDIUM
                                    else -> COVER_TEMPLATE_SIZE_NORMAL
                                },
                            )
                            .align(Alignment.Center),
                        colorFilter = ColorFilter.tint(
                            tint?.let { Color(it) } ?: CoverPlaceholderOnBgColor,
                        ),
                    )
                }
            },
            onSuccess = { result ->
                succeed = true
                if (onCoverLoaded != null) {
                    when (data) {
                        is Anime -> onCoverLoaded(data.asAnimeCover(), result)
                        is DomainAnimeCover -> onCoverLoaded(data, result)
                    }
                }
            },
            // KMK <--
            contentDescription = contentDescription,
            modifier = modifierColored,
            contentScale = scale,
        )
    }

    companion object {
        val COVER_TEMPLATE_SIZE_BIG = 16.dp
        val COVER_TEMPLATE_SIZE_MEDIUM = 24.dp
        val COVER_TEMPLATE_SIZE_NORMAL = 32.dp
    }
}

enum class AnimeCoverHide(private val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    ;

    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
        // KMK -->
        /** background color, which used for loading/error indicator */
        bgColor: Color? = CoverPlaceholderColor,
        /** onBackground color, which used for loading/error indicator */
        @ColorInt tint: Int? = null,
    ) {
        val modifierColored = modifier
            .aspectRatio(ratio)
            .clip(shape)
            .background(bgColor ?: CoverPlaceholderColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )

        Box(
            modifier = modifierColored,
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_baseline_menu_book_24),
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center),
                colorFilter = ColorFilter.tint(
                    tint?.let { Color(it) } ?: CoverPlaceholderOnBgColor,
                ),
            )
        }
    }
}

internal val CoverPlaceholderColor = Color(0x1F888888)
internal val CoverPlaceholderOnBgColor = Color(0x8F888888)
