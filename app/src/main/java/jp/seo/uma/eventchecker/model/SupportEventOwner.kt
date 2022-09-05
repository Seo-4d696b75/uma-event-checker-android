package jp.seo.uma.eventchecker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportEventOwner(
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: String,
    @SerialName("icon")
    val icon: String
)