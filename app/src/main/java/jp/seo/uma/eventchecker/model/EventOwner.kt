package jp.seo.uma.eventchecker.model

import jp.seo.uma.eventchecker.img.EventType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * イベントの所有者・シナリオの参照子
 */
@Serializable(with = EventOwnerSerializer::class)
sealed interface EventOwner {
    val type: String
    val name: String

    @Serializable
    data class Scenario(override val name: String) : EventOwner {
        override val type = "scenario"
    }

    @Serializable
    data class Partner(val id: Int, override val name: String) : EventOwner {
        override val type = "chara"
    }

    @Serializable
    data class SupportCard(val id: Int, override val name: String) : EventOwner {
        override val type = "support"
    }
}

fun EventOwner.match(type: EventType) = when (this) {
    is EventOwner.Scenario -> type == EventType.Scenario
    is EventOwner.Partner -> type == EventType.Partner
    is EventOwner.SupportCard -> type == EventType.SupportCard
}

class EventOwnerSerializer : KSerializer<EventOwner> {
    override fun deserialize(decoder: Decoder): EventOwner {
        require(decoder is JsonDecoder)
        val e = decoder.decodeJsonElement()
        require(e is JsonObject)
        val serializer = when (val type = e["type"]?.jsonPrimitive?.content) {
            "scenario" -> EventOwner.Scenario.serializer()
            "chara" -> EventOwner.Partner.serializer()
            "support" -> EventOwner.SupportCard.serializer()
            else -> throw IllegalArgumentException("invalid value:$type for event owner type")
        }
        return decoder.json.decodeFromJsonElement(serializer, e)
    }

    override fun serialize(encoder: Encoder, value: EventOwner) {
        when (value) {
            is EventOwner.Scenario -> {
                encoder.encodeSerializableValue(EventOwner.Scenario.serializer(), value)
            }
            is EventOwner.Partner -> {
                encoder.encodeSerializableValue(EventOwner.Partner.serializer(), value)
            }
            is EventOwner.SupportCard -> {
                encoder.encodeSerializableValue(EventOwner.SupportCard.serializer(), value)
            }
        }
    }

    override val descriptor = buildClassSerialDescriptor("owner") {
        element("type", serialDescriptor<String>())
        element("name", serialDescriptor<String>())
        element("id", serialDescriptor<Int>().nullable)
    }

}