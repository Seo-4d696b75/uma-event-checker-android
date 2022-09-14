package jp.seo.uma.eventchecker.model

import jp.seo.uma.eventchecker.repository.normalizeForComparison
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
    val owner: EventOwner,
    @SerialName("title_kana")
    val titleKana: String,
    @SerialName("choices")
    val choices: List<EventChoice>
) : java.io.Serializable {

    companion object {
        private val pattern = Regex("(?<origin>レース.+?)\\([0-9].+?\\)")
    }

    // titleテキストに一部実際に表示されない文字が含まれる
    val normalizedTitle: String = pattern.matchEntire(title).let { matcher ->
        matcher?.let { it.groupValues[0] } ?: title
    }.normalizeForComparison()

    override fun toString(): String {
        return "$title\n${
            choices.joinToString(
                separator = "\n",
                transform = EventChoice::toString
            )
        }"
    }
}
