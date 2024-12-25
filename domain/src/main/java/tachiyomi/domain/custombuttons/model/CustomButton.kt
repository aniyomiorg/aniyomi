package tachiyomi.domain.custombuttons.model

data class CustomButton(
    val id: Long,
    val name: String,
    val isFavorite: Boolean,
    val sortIndex: Long,
    val content: String,
    val longPressContent: String,
    val onStartup: String,
) {
    fun getButtonContent(): String {
        return content.replace("${'$'}id", id.toString())
    }

    fun getButtonLongPressContent(): String {
        return longPressContent.replace("${'$'}id", id.toString())
    }

    fun getButtonOnStartup(): String {
        return onStartup.replace("${'$'}id", id.toString())
    }
}
