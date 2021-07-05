package jp.seo.uma.eventchecker.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Seo-4d696b75
 * @version 2021/07/04.
 */

@Serializable
data class GameEvent(
    @SerialName("e")
    val eventTitle: String,
    @SerialName("n")
    val ownerName: String,
    @SerialName("c")
    val eventClass: String,
    @SerialName("k")
    val eventTitleKana: String,
    @SerialName("choices")
    val choices: Array<EventChoice>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameEvent

        if (eventTitle != other.eventTitle) return false
        if (ownerName != other.ownerName) return false
        if (eventClass != other.eventClass) return false
        if (eventTitleKana != other.eventTitleKana) return false
        if (!choices.contentEquals(other.choices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventTitle.hashCode()
        result = 31 * result + ownerName.hashCode()
        result = 31 * result + eventClass.hashCode()
        result = 31 * result + eventTitleKana.hashCode()
        result = 31 * result + choices.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "$eventTitle\n${
            choices.joinToString(
                separator = "\n",
                transform = EventChoice::toString
            )
        }"
    }
}

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
