package jp.seo.uma.eventchecker.repository

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.api.EventDataInfo
import jp.seo.uma.eventchecker.api.NetworkClient
import jp.seo.uma.eventchecker.img.EventType
import jp.seo.uma.eventchecker.img.saveFile
import jp.seo.uma.eventchecker.model.EventOwners
import jp.seo.uma.eventchecker.model.GameEvent
import jp.seo.uma.eventchecker.model.GameEventData
import jp.seo.uma.eventchecker.model.match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.lucene.search.spell.LevensteinDistance
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

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
    private val network: NetworkClient
) {

    companion object {
        const val DATA_FILE = "data.json"
        const val KEY_DATA_VERSION = "data_version"
    }

    private var events: List<GameEvent> = emptyList()
    var eventOwners: EventOwners = EventOwners(emptyList(), emptyList())
        private set

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized

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

    suspend fun clearData() = withContext(Dispatchers.IO) {
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        dataVersion = 0L
        Log.d("clear", "data cleared version:0")
    }

    suspend fun checkUpdate(): EventDataInfo? = withContext(Dispatchers.IO) {
        val info = network.getDataInfo()
        Log.d("checkUpdate", "current:$dataVersion, found:${info.version}")
        if (info.version > dataVersion) {
            info
        } else null
    }

    private val _updateState = MutableStateFlow<DataUpdateState?>(null)
    val updateState: StateFlow<DataUpdateState?> = _updateState

    /**
     * ネットワークから最新のデータを取得してローカルに保存
     */
    suspend fun updateData(
        info: EventDataInfo,
    ) = withContext(Dispatchers.IO) {
        _updateState.update {
            DataUpdateState(context.getString(R.string.update_status_data), 0)
        }
        val data = network.getData {
            val percent = (it * 100f / info.size).toInt()
            _updateState.update {
                DataUpdateState(
                    context.getString(R.string.update_status_data),
                    min(100, percent),
                )
            }
        }
        events = data.events
        eventOwners = data.owners
        val dir = context.filesDir
        File(dir, DATA_FILE).writeText(json.encodeToString(data), Charsets.UTF_8)
        val iconDir = File(dir, "icon")
        if (!iconDir.exists() || !iconDir.isDirectory) {
            if (!iconDir.mkdir()) {
                throw RuntimeException("fail to mkdir: $iconDir")
            }
        }
        val icons = mutableListOf<String>()
        data.owners.supportCards.forEach { icons.add(it.icon) }
        data.owners.partners.forEach { icons.addAll(it.icon) }
        val targets = icons.filter { !File(iconDir, it).exists() }
        targets.forEachParallel(
            process = { s ->
                val res = network.getIconImage(s)
                res.saveFile(File(iconDir, s))
            },
            onProcessed = { _, index ->
                _updateState.update {
                    DataUpdateState(
                        context.getString(
                            R.string.update_status_icons,
                            index + 1,
                            targets.size
                        ),
                        (index * 100f / targets.size).toInt(),
                    )
                }
            },
            context = Dispatchers.IO
        )
        dataVersion = info.version
        _initialized.update { true }
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadData() = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, DATA_FILE)
        if (!file.exists() || !file.isFile) throw IllegalStateException(context.getString(R.string.error_data_not_found))
        val str = file.readText(Charsets.UTF_8)
        val data = json.decodeFromString<GameEventData>(str)
        events = data.events
        eventOwners = data.owners
        _initialized.update { true }
    }

    /**
     * 指定した閾値以上のscoreのイベントを検索してソートしたリストを返す
     */
    fun searchForEvent(title: String, threshold: Float, type: EventType): List<SearchResult> {
        val filtered = events.filter { it.owner.match(type) }
        val result = search(title, filtered)
        val list = result.filter { it.score > threshold }
        return if (list.isNotEmpty()) {
            val maxScore = list.maxByOrNull { it.score }
            Log.d(
                "EventData",
                "search -> max score $maxScore, size ${list.size}, events[0]: ${list[0].event.title}"
            )
            list.sortedByScore()
        } else {
            val maxScore = result.maxByOrNull { it.score }
            Log.d(
                "EventData",
                "search -> max score: $maxScore < th: $threshold"
            )
            emptyList()
        }
    }

    /**
     * 指定した最大の長さで、イベントをscoreでソートしたリストを返す
     */
    fun searchForEvent(title: String, maxSize: Int): List<SearchResult> {
        val result = search(title, events)
        return if (maxSize * 5 < result.size) {
            val src = result.toMutableList()
            val dst = mutableListOf<SearchResult>()
            while (dst.size < maxSize && src.isNotEmpty()) {
                var idx = 0
                var max = 0f
                src.forEachIndexed { i, r ->
                    if (r.score > max) {
                        max = r.score
                        idx = i
                    }
                }
                dst.add(src[idx])
                src.removeAt(idx)
            }
            dst
        } else {
            result.sortedByScore().subList(0, min(maxSize, result.size))
        }
    }

    private fun search(title: String, list: List<GameEvent>): List<SearchResult> {
        val query = title.normalizeForComparison()
        if (query.isEmpty()) return emptyList()
        Log.d("EventData", "normalized query '$query'")
        val algo = LevensteinDistance()
        return list.map {
            val score = algo.getDistance(it.normalizedTitle, query)
            SearchResult(it, score)
        }
    }
}

data class SearchResult(
    val event: GameEvent,
    val score: Float,
)

fun List<SearchResult>.sortedByScore() = sortedBy { -it.score }
