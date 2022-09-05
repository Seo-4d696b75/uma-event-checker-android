package jp.seo.uma.eventchecker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharaEventOwner(
    @SerialName("name")
    val name: String,
    @SerialName("icon")
    val icon: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharaEventOwner

        if (name != other.name) return false
        if (!icon.contentEquals(other.icon)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + icon.contentHashCode()
        return result
    }
}