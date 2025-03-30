package eu.kanade.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.util.animateElevation
import androidx.compose.material3.SuggestionChipDefaults as SuggestionChipDefaultsM3

@ExperimentalMaterial3Api
@Composable
fun SuggestionChip(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder? = SuggestionChipDefaults.suggestionChipBorder(),
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
) = Chip(
    modifier = modifier,
    label = label,
    labelTextStyle = MaterialTheme.typography.labelLarge,
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = icon,
    avatar = null,
    trailingIcon = null,
    leadingIconColor = colors.leadingIconContentColor(enabled).value,
    trailingIconColor = colors.trailingIconContentColor(enabled).value,
    containerColor = colors.containerColor(enabled).value,
    tonalElevation = elevation?.tonalElevation(enabled, interactionSource)?.value ?: 0.dp,
    shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
    minHeight = SuggestionChipDefaultsM3.Height,
    paddingValues = SuggestionChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
)

@ExperimentalMaterial3Api
@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder? = SuggestionChipDefaults.suggestionChipBorder(),
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
) = Chip(
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.labelLarge,
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = icon,
    avatar = null,
    trailingIcon = null,
    leadingIconColor = colors.leadingIconContentColor(enabled).value,
    trailingIconColor = colors.trailingIconContentColor(enabled).value,
    containerColor = colors.containerColor(enabled).value,
    tonalElevation = elevation?.tonalElevation(enabled, interactionSource)?.value ?: 0.dp,
    shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
    minHeight = SuggestionChipDefaultsM3.Height,
    paddingValues = SuggestionChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
    interactionSource = interactionSource,
)

@Suppress("SameParameterValue")
@ExperimentalMaterial3Api
@Composable
private fun Chip(
    modifier: Modifier,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    leadingIconColor: Color,
    trailingIconColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    minHeight: Dp,
    paddingValues: PaddingValues,
    shape: Shape,
    border: BorderStroke?,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            labelColor = labelColor,
            leadingIcon = leadingIcon,
            avatar = avatar,
            trailingIcon = trailingIcon,
            leadingIconColor = leadingIconColor,
            trailingIconColor = trailingIconColor,
            minHeight = minHeight,
            paddingValues = paddingValues,
        )
    }
}

@Suppress("SameParameterValue")
@ExperimentalMaterial3Api
@Composable
private fun Chip(
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    enabled: Boolean,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    leadingIconColor: Color,
    trailingIconColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    minHeight: Dp,
    paddingValues: PaddingValues,
    shape: Shape,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource,
) {
    tachiyomi.presentation.core.components.material.Surface(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = interactionSource,
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            labelColor = labelColor,
            leadingIcon = leadingIcon,
            avatar = avatar,
            trailingIcon = trailingIcon,
            leadingIconColor = leadingIconColor,
            trailingIconColor = trailingIconColor,
            minHeight = minHeight,
            paddingValues = paddingValues,
        )
    }
}

@Composable
private fun ChipContent(
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    leadingIconColor: Color,
    trailingIconColor: Color,
    minHeight: Dp,
    paddingValues: PaddingValues,
) {
    CompositionLocalProvider(
        LocalContentColor provides labelColor,
        LocalTextStyle provides labelTextStyle,
    ) {
        Row(
            Modifier.defaultMinSize(minHeight = minHeight).padding(paddingValues),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatar != null) {
                avatar()
            } else if (leadingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides leadingIconColor,
                    content = leadingIcon,
                )
            }
            Spacer(Modifier.width(HorizontalElementsPadding))
            label()
            Spacer(Modifier.width(HorizontalElementsPadding))
            if (trailingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides trailingIconColor,
                    content = trailingIcon,
                )
            }
        }
    }
}

/**
 * Contains the baseline values used by [SuggestionChip].
 */
@ExperimentalMaterial3Api
object SuggestionChipDefaults {

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * a flat [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun suggestionChipColors(
        containerColor: Color = Color.Transparent,
        labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor: Color = Color.Transparent,
        disabledLabelColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.38f),
        disabledIconContentColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.38f),
    ): ChipColors = ChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconContentColor = iconContentColor,
        trailingIconContentColor = Color.Unspecified,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconContentColor = disabledIconContentColor,
        disabledTrailingIconContentColor = Color.Unspecified,
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for a flat [SuggestionChip].
     *
     * @param defaultElevation the elevation used when the chip is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun suggestionChipElevation(
        defaultElevation: Dp = 0.0.dp,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = 8.0.dp,
        disabledElevation: Dp = defaultElevation,
    ): ChipElevation = ChipElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )

    /**
     * Creates a [ChipBorder] that represents the default border used in a flat [SuggestionChip].
     *
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Composable
    fun suggestionChipBorder(
        borderColor: Color = MaterialTheme.colorScheme.outline,
        disabledBorderColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.12f),
        borderWidth: Dp = 1.0.dp,
    ): ChipBorder = ChipBorder(
        borderColor = borderColor,
        disabledBorderColor = disabledBorderColor,
        borderWidth = borderWidth,
    )

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun elevatedSuggestionChipColors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.12f),
        disabledLabelColor: Color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.38f),
        disabledIconContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    ): ChipColors = ChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconContentColor = iconContentColor,
        trailingIconContentColor = Color.Unspecified,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconContentColor = disabledIconContentColor,
        disabledTrailingIconContentColor = Color.Unspecified,
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for an elevated [SuggestionChip].
     *
     * @param defaultElevation the elevation used when the chip is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedSuggestionChipElevation(
        defaultElevation: Dp = 1.0.dp,
        pressedElevation: Dp = 1.0.dp,
        focusedElevation: Dp = 1.0.dp,
        hoveredElevation: Dp = 3.0.dp,
        draggedElevation: Dp = 8.0.dp,
        disabledElevation: Dp = 0.0.dp,
    ): ChipElevation = ChipElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation,
        draggedElevation = draggedElevation,
        disabledElevation = disabledElevation,
    )
}

/**
 * Represents the container and content colors used in a clickable chip in different states.
 *
 * See [AssistChipDefaults], [InputChipDefaults], and [SuggestionChipDefaults] for the default
 * colors used in the various Chip configurations.
 */
@ExperimentalMaterial3Api
@Immutable
class ChipColors internal constructor(
    private val containerColor: Color,
    private val labelColor: Color,
    private val leadingIconContentColor: Color,
    private val trailingIconContentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledLabelColor: Color,
    private val disabledLeadingIconContentColor: Color,
    private val disabledTrailingIconContentColor: Color,
) {
    /**
     * Represents the container color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
    }

    /**
     * Represents the label color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun labelColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) labelColor else disabledLabelColor)
    }

    /**
     * Represents the leading icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun leadingIconContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) leadingIconContentColor else disabledLeadingIconContentColor,
        )
    }

    /**
     * Represents the trailing icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun trailingIconContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) trailingIconContentColor else disabledTrailingIconContentColor,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipColors) return false

        if (containerColor != other.containerColor) return false
        if (labelColor != other.labelColor) return false
        if (leadingIconContentColor != other.leadingIconContentColor) return false
        if (trailingIconContentColor != other.trailingIconContentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (disabledLeadingIconContentColor != other.disabledLeadingIconContentColor) return false
        if (disabledTrailingIconContentColor != other.disabledTrailingIconContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + leadingIconContentColor.hashCode()
        result = 31 * result + trailingIconContentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + disabledLeadingIconContentColor.hashCode()
        result = 31 * result + disabledTrailingIconContentColor.hashCode()

        return result
    }
}

/**
 * Represents the border stroke used in a chip in different states.
 */
@ExperimentalMaterial3Api
@Immutable
class ChipBorder internal constructor(
    private val borderColor: Color,
    private val disabledBorderColor: Color,
    private val borderWidth: Dp,
) {
    /**
     * Represents the [BorderStroke] for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun borderStroke(enabled: Boolean): State<BorderStroke?> {
        return rememberUpdatedState(
            BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipBorder) return false

        if (borderColor != other.borderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (borderWidth != other.borderWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + borderWidth.hashCode()

        return result
    }
}

/**
 * Represents the elevation for a chip in different states.
 */
@ExperimentalMaterial3Api
@Immutable
class ChipElevation internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the tonal elevation used in a chip, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is [ColorScheme.surface], a higher elevation will result in a darker
     * color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the chip.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    internal fun tonalElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    /**
     * Represents the shadow elevation used in a chip, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the chip to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target = if (!enabled) {
            disabledElevation
        } else {
            when (interaction) {
                is PressInteraction.Press -> pressedElevation
                is HoverInteraction.Enter -> hoveredElevation
                is FocusInteraction.Focus -> focusedElevation
                is DragInteraction.Start -> draggedElevation
                else -> defaultElevation
            }
        }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        if (!enabled) {
            // No transition when moving to a disabled state
            LaunchedEffect(target) { animatable.snapTo(target) }
        } else {
            LaunchedEffect(target) {
                val lastInteraction = when (animatable.targetValue) {
                    pressedElevation -> PressInteraction.Press(Offset.Zero)
                    hoveredElevation -> HoverInteraction.Enter()
                    focusedElevation -> FocusInteraction.Focus()
                    draggedElevation -> DragInteraction.Start()
                    else -> null
                }
                animatable.animateElevation(
                    from = lastInteraction,
                    to = interaction,
                    target = target,
                )
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

/**
 * The padding between the elements in the chip.
 */
private val HorizontalElementsPadding = 8.dp

/**
 * Returns the [PaddingValues] for the suggestion chip.
 */
private val SuggestionChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)
