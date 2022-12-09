package jp.seo.uma.eventchecker.model

import android.content.Context
import android.util.Log
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.readFloat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.lucene.search.spell.LevensteinDistance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ゲーム中のイベント情報を管理・検索
 *
 * @author Seo-4d696b75
 * @version 2021/07/04.
 */
@Singleton
class DataRepository @Inject constructor() {

    companion object {
        const val DATA_FILE = "event.json"
    }

    private lateinit var events: Array<GameEvent>
    private var ocrThreshold: Float = 0.5f

    private val json = Json { ignoreUnknownKeys = true }

    private val _initialized = MutableStateFlow(false)
    val initialized = _initialized.asStateFlow()

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (_initialized.value) return@withContext
        val manager = context.resources.assets
        manager.open(DATA_FILE).use { reader ->
            val str = reader.readBytes().toString(Charsets.UTF_8)
            events = json.decodeFromString(str)
        }
        ocrThreshold = context.resources.readFloat(R.dimen.ocr_title_threshold)
        _initialized.update { true }
    }

    fun searchEvent(title: String): GameEvent? {
        val algo = LevensteinDistance()
        val score = events.map { event -> algo.getDistance(event.title, title) }
        return score.maxOrNull()?.let { maxScore ->
            if (maxScore > ocrThreshold) {
                val list = events.toList().filterIndexed { idx, e -> score[idx] >= maxScore }
                Log.d(
                    "search",
                    "max score: $maxScore size: ${list.size} events[0]: ${list[0].title}"
                )
                list[0]
            } else {
                Log.d(
                    "search",
                    "max score: $maxScore < th: $ocrThreshold"
                )
                null
            }
        }
    }
}

@Serializable
data class GameEvent(
    @SerialName("e")
    val title: String,
    @SerialName("n")
    val ownerName: String,
    @SerialName("k")
    val titleKana: String,
    @SerialName("choices")
    val choices: Array<EventChoice>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameEvent

        if (title != other.title) return false
        if (ownerName != other.ownerName) return false
        if (titleKana != other.titleKana) return false
        if (!choices.contentEquals(other.choices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + ownerName.hashCode()
        result = 31 * result + titleKana.hashCode()
        result = 31 * result + choices.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "$title\n${
            choices.joinToString(
                separator = "\n",
                transform = EventChoice::toString
            )
        }"
    }
}

@Serializable
data class EventChoice(
    @SerialName("n")
    val name: String,
    @SerialName("t")
    val message: String
) {
    override fun toString(): String {
        return "- $name\n  ${formatMessage("\n  ")}"
    }

    fun formatMessage(separator: String = "\n"): String {
        val lines = message.split("[br]", "<hr>")
        return lines.joinToString(separator = separator)
    }
}
