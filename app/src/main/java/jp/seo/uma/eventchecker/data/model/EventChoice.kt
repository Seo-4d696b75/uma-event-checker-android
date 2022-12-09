package jp.seo.uma.eventchecker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventChoice(
    @SerialName("n")
    val name: String,
    @SerialName("t")
    val message: String
) {
    override fun toString(): String {
        return "- $name\n  ${formatMessage("\n  ")}"
    }

    fun formatMessage(separator: String = "\n"): String {
        val lines = message.split("[br]", "<hr>")
        return lines.joinToString(separator = separator)
    }
}
