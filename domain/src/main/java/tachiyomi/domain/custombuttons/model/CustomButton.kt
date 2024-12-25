package tachiyomi.domain.custombuttons.model

data class CustomButton(
    val id: Long,
    val name: String,
    val isFavorite: Boolean,
    val sortIndex: Long,
    val content: String,
    val longPressContent: String,
    val onStartup: String,
)
