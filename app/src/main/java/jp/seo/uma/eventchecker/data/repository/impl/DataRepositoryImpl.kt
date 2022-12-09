package jp.seo.uma.eventchecker.data.repository.impl

import android.content.Context
import android.util.Log
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.data.model.GameEvent
import jp.seo.uma.eventchecker.data.repository.DataRepository
import jp.seo.uma.eventchecker.readFloat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.lucene.search.spell.LevensteinDistance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepositoryImpl @Inject constructor() : DataRepository {

    companion object {
        const val DATA_FILE = "event.json"
    }

    private lateinit var events: Array<GameEvent>
    private var ocrThreshold: Float = 0.5f

    private val json = Json { ignoreUnknownKeys = true }

    private val _initialized = MutableStateFlow(false)
    override val initialized = _initialized.asStateFlow()

    override suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (_initialized.value) return@withContext
        val manager = context.resources.assets
        manager.open(DATA_FILE).use { reader ->
            val str = reader.readBytes().toString(Charsets.UTF_8)
            events = json.decodeFromString(str)
        }
        ocrThreshold = context.resources.readFloat(R.dimen.ocr_title_threshold)
        _initialized.update { true }
    }

    override fun searchEvent(title: String): GameEvent? {
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