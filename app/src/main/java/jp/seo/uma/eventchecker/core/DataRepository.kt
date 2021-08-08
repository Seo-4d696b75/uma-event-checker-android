package jp.seo.uma.eventchecker.core

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.lucene.search.spell.LevensteinDistance
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ゲーム中のイベント情報を管理・検索
 *
 * @author Seo-4d696b75
 * @version 2021/07/04.
 */
@Singleton
class DataRepository @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val network: DataNetwork
) {

    companion object {
        const val DATA_FILE = "data.json"
        const val KEY_DATA_VERSION = "data_version"
    }

    private var events: Array<GameEvent> = emptyArray()
    private var eventOwners: EventOwners = EventOwners(emptyArray(), emptyArray())
    private var ocrThreshold: Float = context.resources.readFloat(R.dimen.ocr_title_threshold)
    private val _currentEvent = MutableLiveData<GameEvent?>(null)
    private val _initialized = MutableLiveData(false)

    private val preferences = context.getSharedPreferences("main", MODE_PRIVATE)

    var dataVersion = preferences.getLong(KEY_DATA_VERSION, 0L)
        private set(value) {
            if (field == value) return
            field = value
            preferences.edit().also {
                it.putLong(KEY_DATA_VERSION, value)
                it.apply()
            }
        }

    suspend fun checkUpdate(): EventDataInfo? = withContext(Dispatchers.IO) {
        val info = network.getDataInfo()
        if (info.version > dataVersion) {
            info
        } else null
    }

    suspend fun updateData(info: EventDataInfo) = withContext(Dispatchers.IO) {
        val data = network.getData()
        events = data.events
        eventOwners = data.owners
        val dir = context.filesDir
        File(dir, DATA_FILE).writeText(Json.encodeToString(data), Charsets.UTF_8)
        val iconDir = File(dir, "icon")
        if (!iconDir.exists() || !iconDir.isDirectory) {
            if (!iconDir.mkdir()) {
                throw RuntimeException("fail to mkdir: $iconDir")
            }
        }
        val icons = mutableListOf<String>()
        data.owners.supportEventOwners.forEach { icons.add(it.icon) }
        data.owners.charaEventOwners.forEach { icons.addAll(it.icon) }
        icons.filter { !File(iconDir, it).exists() }.forEach {
            val res = network.getIconImage(it)
            res.saveFile(File(iconDir, it))
        }
        dataVersion = info.version
        _initialized.postValue(true)
    }

    suspend fun loadData() = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, DATA_FILE)
        if (!file.exists() || !file.isFile) throw IllegalStateException("event data not initialized yet")
        val str = file.readText(Charsets.UTF_8)
        val data = Json { ignoreUnknownKeys = true }.decodeFromString<GameEventData>(str)
        events = data.events
        eventOwners = data.owners
        _initialized.postValue(true)
    }

    private var eventTitle: String? = null

    fun setCurrentEvent(events: List<GameEvent>, ownerName: String?) {
        if (events.isEmpty()) {
            _currentEvent.postValue(null)
        } else if (events.size == 1 || ownerName == null) {
            _currentEvent.postValue(events[0])
        } else {
            var event = events[0]
            val filter = events.filter { it.ownerName == ownerName }
            if (filter.isNotEmpty()) {
                event = filter[0]
                Log.d("EventData", "filtered size ${filter.size}, [0]-> ${event.title}")
            } else {
                Log.d("EventData", "no event remains after filter")
            }
            _currentEvent.postValue(event)
        }
    }

    val currentEvent: LiveData<GameEvent?> = _currentEvent
    val initialized: LiveData<Boolean> = _initialized

    suspend fun searchForEvent(title: String?): List<GameEvent>? {
        if (eventTitle != title) {
            eventTitle = title
            return if (title == null) {
                emptyList()
            } else {
                searchEventTitle(title.normalizeForComparison())
            }
        }
        return null
    }

    private suspend fun searchEventTitle(title: String): List<GameEvent> {
        Log.d("EventData", "normalized query '$title'")
        val score = Array<Float>(events.size) { 0f }
        calcTitleDistance(0, events.size, title, score)
        return score.maxOrNull()?.let { maxScore ->
            if (maxScore > ocrThreshold) {
                val list = events.toList().filterIndexed { idx, e -> score[idx] >= maxScore }
                Log.d(
                    "EventData",
                    "search -> max score $maxScore, size ${list.size}, events[0]: ${list[0].title}"
                )
                list
            } else {
                Log.d(
                    "EventData",
                    "search -> max score: $maxScore < th: $ocrThreshold"
                )
                null
            }
        } ?: emptyList()
    }

    private suspend fun calcTitleDistance(
        start: Int,
        end: Int,
        query: String,
        dst: Array<Float>
    ): Unit = withContext(Dispatchers.Default) {
        if (start + 64 < end) {
            val mid = start + (end - start) / 2
            val left = async {
                calcTitleDistance(start, mid, query, dst)
            }
            val right = async {
                calcTitleDistance(mid, end, query, dst)
            }
            left.await()
            right.await()
        } else {
            val algo = LevensteinDistance()
            (start until end).forEach { idx ->
                val event = events[idx]
                dst[idx] = algo.getDistance(event.normalizedTitle, query)
            }
        }
    }

}
