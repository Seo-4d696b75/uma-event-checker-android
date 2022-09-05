package jp.seo.uma.eventchecker.model

import jp.seo.uma.eventchecker.core.normalizeForComparison
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */


@Serializable
data class GameEvent(
    @SerialName("title")
    val title: String,
    @SerialName("owner")
    val ownerName: String,
    @SerialName("title_kana")
    val titleKana: String,
    @SerialName("choices")
    val choices: Array<EventChoice>
) {

    companion object {
        private val pattern = Regex("(?<origin>レース.+?)\\([0-9].+?\\)")
    }

    // titleテキストに一部実際に表示されない文字が含まれる
    val normalizedTitle: String = pattern.matchEntire(title).let { matcher ->
        matcher?.let { it.groupValues[0] } ?: title
    }.normalizeForComparison()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameEvent

        if (title != other.title) return false
        if (ownerName != other.ownerName) return false
        if (titleKana != other.titleKana) return false
        if (!choices.contentEquals(other.choices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + ownerName.hashCode()
        result = 31 * result + titleKana.hashCode()
        result = 31 * result + choices.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "$title\n${
            choices.joinToString(
                separator = "\n",
                transform = EventChoice::toString
            )
        }"
    }
}