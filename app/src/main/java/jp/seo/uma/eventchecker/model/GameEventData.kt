package jp.seo.uma.eventchecker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GameEventData(
    @SerialName("event")
    val events: Array<GameEvent>,
    @SerialName("owner")
    val owners: EventOwners
)
