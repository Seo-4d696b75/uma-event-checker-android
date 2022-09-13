package jp.seo.uma.eventchecker.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 育成キャラ・サポートカード
 *
 * 共通の属性を定義
 */
sealed interface Uma {
    val id: Int
    val name: String
}

/**
 * 育成キャラ
 */
@Serializable
data class Partner(
    override val id: Int,
    override val name: String,
    val icon: Array<String>
) : Uma {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Partner

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

/**
 * サポカ
 */
@Serializable
data class SupportCard(
    override val id: Int,
    override val name: String,
    val icon: String,
    val type: SupportType,
    val rarity: Rarity,
) : Uma

enum class Rarity {
    R, SR, SSR,
}

@Serializable(with = SupportTypeSerializer::class)
enum class SupportType {
    Speed,
    Stamina,
    Power,
    Guts,
    Smartness,
    Friend,
    Group,
}

class SupportTypeSerializer : KSerializer<SupportType> {
    override fun deserialize(decoder: Decoder): SupportType {
        return when (val value = decoder.decodeString()) {
            "スピ" -> SupportType.Speed
            "スタ" -> SupportType.Stamina
            "パワ" -> SupportType.Power
            "根性" -> SupportType.Guts
            "賢さ" -> SupportType.Smartness
            "友人" -> SupportType.Friend
            "グル" -> SupportType.Group
            else -> throw IllegalArgumentException("invalid value:$value for support type")
        }
    }

    override val descriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SupportType) {
        when (value) {
            SupportType.Speed -> encoder.encodeString("スピ")
            SupportType.Stamina -> encoder.encodeString("スタ")
            SupportType.Power -> encoder.encodeString("パワ")
            SupportType.Guts -> encoder.encodeString("根性")
            SupportType.Smartness -> encoder.encodeString("賢さ")
            SupportType.Friend -> encoder.encodeString("友人")
            SupportType.Group -> encoder.encodeString("グル")
        }
    }
}