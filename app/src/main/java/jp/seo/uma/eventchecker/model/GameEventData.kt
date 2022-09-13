package jp.seo.uma.eventchecker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GameEventData(
    @SerialName("event")
    val events: List<GameEvent>,
    @SerialName("owner")
    val owners: EventOwners
)
