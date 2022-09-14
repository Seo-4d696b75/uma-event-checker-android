package jp.seo.uma.eventchecker.model

import jp.seo.uma.eventchecker.img.EventType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * イベントの所有者・シナリオの参照子
 */
@Serializable(with = EventOwnerSerializer::class)
sealed interface EventOwner {
    val name: String

    @Serializable
    data class Scenario(override val name: String) : EventOwner

    @Serializable
    data class Partner(val id: Int, override val name: String) : EventOwner

    @Serializable
    data class SupportCard(val id: Int, override val name: String) : EventOwner
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
                encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, "scenario")
                    encodeStringElement(descriptor, 1, value.name)
                }
            }
            is EventOwner.Partner -> {
                encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, "chara")
                    encodeStringElement(descriptor, 1, value.name)
                    encodeIntElement(descriptor, 2, value.id)
                }
            }
            is EventOwner.SupportCard -> {
                encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, "support")
                    encodeStringElement(descriptor, 1, value.name)
                    encodeIntElement(descriptor, 2, value.id)
                }
            }
        }
    }

    override val descriptor = buildClassSerialDescriptor("owner") {
        element("type", serialDescriptor<String>())
        element("name", serialDescriptor<String>())
        element("id", serialDescriptor<Int>().nullable)
    }

}