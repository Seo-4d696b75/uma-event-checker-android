package jp.seo.uma.eventchecker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EventOwners(
    @SerialName("chara")
    val partners: List<Partner>,
    @SerialName("support")
    val supportCards: List<SupportCard>
)