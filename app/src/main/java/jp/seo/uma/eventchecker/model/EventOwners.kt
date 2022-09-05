package jp.seo.uma.eventchecker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EventOwners(
    @SerialName("chara")
    val charaEventOwners: Array<CharaEventOwner>,
    @SerialName("support")
    val supportEventOwners: Array<SupportEventOwner>
)