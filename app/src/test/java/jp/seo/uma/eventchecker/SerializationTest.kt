package jp.seo.uma.eventchecker

import jp.seo.uma.eventchecker.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserializeEvent() {
        val str = """
        {
            "title": "イベント名",
            "owner": {
                "type": "scenario",
                "name": "main"
            },
            "title_kana": "いべんとめい",
            "choices": [
                {
                    "name": "選択肢",
                    "message": "選択時の効果"
                }
            ]
         }
        """.trimIndent()
        val event = json.decodeFromString<GameEvent>(str)
        assertThat(event.title, `is`("イベント名"))
        assertThat(event.owner.name, `is`("main"))
        assertThat(event.choices.size, `is`(1))
    }

    @Test
    fun serializeEvent() {
        val event = GameEvent(
            title = "タイトル",
            titleKana = "たいとる",
            owner = EventOwner.Scenario(name = "main"),
            choices = arrayOf(),
        )
        val str = json.encodeToString(event)
        val obj = json.decodeFromString<GameEvent>(str)
        assertThat(obj, `is`(event))
    }

    @Test
    fun deserializeSupportCard() {
        val str = """
        {
            "id": 1,
            "name": "サポカ",
            "icon": "icon_name",
            "type": "賢さ",
            "rarity": "SSR"
        }
        """.trimIndent()
        val s = json.decodeFromString<SupportCard>(str)
        assertThat(s.rarity, `is`(Rarity.SSR))
        assertThat(s.type, `is`(SupportType.Smartness))
    }

    @Test
    fun serializeSupportCard() {
        val card = SupportCard(
            id = 1,
            name = "サポカ",
            icon = "icon",
            rarity = Rarity.R,
            type = SupportType.Friend,
        )
        val str = json.encodeToString(card)
        val obj = json.decodeFromString<SupportCard>(str)
        assertThat(obj, `is`(card))
    }
}