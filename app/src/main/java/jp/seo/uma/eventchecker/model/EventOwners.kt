package jp.seo.uma.eventchecker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EventOwners(
    @SerialName("chara")
    val partners: Array<Partner>,
    @SerialName("support")
    val supportCards: Array<SupportCard>
)