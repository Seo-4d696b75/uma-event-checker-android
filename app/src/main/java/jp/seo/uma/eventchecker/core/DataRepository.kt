package jp.seo.uma.eventchecker.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.lucene.search.spell.LevensteinDistance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/04.
 */
@Singleton
class DataRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    companion object {
        const val DATA_FILE = "event.json"
    }

    private val events: Array<GameEvent>
    private val ocrThreshold: Float
    private val _currentEvent = MutableLiveData<GameEvent?>(null)


    init {
        val manager = context.resources.assets
        manager.open(DATA_FILE).use { reader ->
            val str = reader.readBytes().toString(Charsets.UTF_8)
            events = Json { ignoreUnknownKeys = true }.decodeFromString(str)
        }
        ocrThreshold = context.resources.readFloat(R.dimen.ocr_title_threshold)
    }

    private var eventTitle: String? = null

    fun setEventTitle(value: String?) {
        if (eventTitle != value) {
            if (value == null) {
                _currentEvent.postValue(null)
            } else {
                val event = searchEventTitle(value)
                _currentEvent.postValue(event)
            }
        }
        eventTitle = value
    }

    val currentEvent: LiveData<GameEvent?> = _currentEvent

    private fun searchEventTitle(title: String): GameEvent? {
        val score = Array<Float>(events.size) { 0f }
        calcTitleDistance(0, events.size, title, score)
        return score.maxOrNull()?.let { maxScore ->
            if (maxScore > ocrThreshold) {
                val list = events.toList().filterIndexed { idx, e -> score[idx] >= maxScore }
                Log.d(
                    "search",
                    "max score: $maxScore size: ${list.size} events[0]: ${list[0].eventTitle}"
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

    private fun calcTitleDistance(start: Int, end: Int, query: String, dst: Array<Float>) {
        if (start + 32 < end) {
            val mid = start + (end - start) / 2
            //viewModelScope.apply {
            //   val left = async(Dispatchers.IO) {
            calcTitleDistance(start, mid, query, dst)
            // }
            //val right = async(Dispatchers.IO) {
            calcTitleDistance(mid, end, query, dst)
            //}
            //left.await()
            //right.await()
            //}
        } else {
            val algo = LevensteinDistance()
            (start until end).forEach { idx ->
                val event = events[idx]
                dst[idx] = algo.getDistance(event.eventTitle, query)
            }
        }
    }

}

@Serializable
data class GameEvent(
    @SerialName("e")
    val eventTitle: String,
    @SerialName("n")
    val ownerName: String,
    @SerialName("c")
    val eventClass: String,
    @SerialName("k")
    val eventTitleKana: String,
    @SerialName("choices")
    val choices: Array<EventChoice>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameEvent

        if (eventTitle != other.eventTitle) return false
        if (ownerName != other.ownerName) return false
        if (eventClass != other.eventClass) return false
        if (eventTitleKana != other.eventTitleKana) return false
        if (!choices.contentEquals(other.choices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventTitle.hashCode()
        result = 31 * result + ownerName.hashCode()
        result = 31 * result + eventClass.hashCode()
        result = 31 * result + eventTitleKana.hashCode()
        result = 31 * result + choices.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "$eventTitle\n${
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
