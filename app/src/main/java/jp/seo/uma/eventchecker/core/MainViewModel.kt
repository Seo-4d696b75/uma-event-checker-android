package jp.seo.uma.eventchecker.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.googlecode.tesseract.android.TessBaseAPI
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.img.*
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.lucene.search.spell.LevensteinDistance
import java.io.File

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
class MainViewModel : ViewModel() {

    companion object {
        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"


        fun getInstance(store: ViewModelStore): MainViewModel {
            val factory = object : ViewModelProvider.Factory {
                @SuppressWarnings("unchecked_cast")
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    val obj = MainViewModel()
                    return obj as T
                }
            }
            return ViewModelProvider({ store }, factory).get(MainViewModel::class.java)
        }
    }

    private val _loading = MutableLiveData(false)

    val loading: LiveData<Boolean> = _loading

    private var ocrText: String? = null

    private var hasInitialized = false
    private lateinit var ocrApi: TessBaseAPI
    private var ocrThreshold: Float = 0f

    private lateinit var headerDetector: TemplateDetector
    private lateinit var charaEventDetector: EventTypeDetector
    private lateinit var supportEventDetector: EventTypeDetector
    private lateinit var mainEventTypeDetector: EventTypeDetector
    private lateinit var eventTitleCropper: EventTitleProcess


    private lateinit var events: Array<GameEvent>
    val currentEvent = MutableLiveData<GameEvent?>(null)

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        if (hasInitialized) return@launch
        _loading.value = true
        loadData(context)
        headerDetector = GameHeaderDetector(context)
        charaEventDetector = EventTypeDetector(
            context.assets.getBitmap("template/event_chara.png").toGrayMat(),
            context
        )
        supportEventDetector = EventTypeDetector(
            context.assets.getBitmap("template/event_support.png").toGrayMat(),
            context
        )
        mainEventTypeDetector = EventTypeDetector(
            context.assets.getBitmap("template/event_main.png").toGrayMat(),
            context
        )
        eventTitleCropper = EventTitleProcess(context)
        ocrThreshold = context.resources.readFloat(R.dimen.ocr_title_threshold)

        hasInitialized = true
        _loading.value = false
    }

    private suspend fun loadData(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, OCR_DATA_DIR)
        if (!dir.exists() || !dir.isDirectory) {
            if (!dir.mkdir()) {
                throw RuntimeException("fail to mkdir: $dir")
            }
        }
        val file = File(dir, OCR_TRAINED_DATA)
        if (!file.exists()) {
            copyAssetsToFiles(context, OCR_TRAINED_DATA, file)
        }
        ocrApi = TessBaseAPI()
        if (!ocrApi.init(context.filesDir.toString(), "jpn")) {
            throw RuntimeException("fail to ocr client")
        }

        val manager = context.resources.assets
        manager.open("event.json").use { reader ->
            val str = reader.readBytes().toString(Charsets.UTF_8)
            events = Json { ignoreUnknownKeys = true }.decodeFromString(str)
        }
    }

    private fun testImage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val src = context.assets.getBitmap("test_game_2.jpg")
            val detect = headerDetector.detect(src)
            Log.d("Header", "detect: $detect")
        }

    }

    private var latestBitmap: Bitmap? = null
    private var processRunning: Boolean = false

    fun updateScreen(img: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        if (processRunning) {
            latestBitmap?.recycle()
            latestBitmap = img
            return@launch
        }
        processRunning = true
        var bitmap: Bitmap? = img
        while (bitmap != null) {
            update(img)
            delay(500L)
            bitmap = latestBitmap
            latestBitmap = null
        }
        processRunning = false
    }

    private suspend fun update(img: Bitmap) {
        val isGame = headerDetector.detect(img)
        Log.d("update", "target $isGame")
        if (isGame) {
            val type = detectEventType(img)
            Log.d("update", "event type '${type.toString()}'")
            if (type != null) {
                val title = getEventTitle(img)
                Log.d("update", "event title '$title'")
                val latest = ocrText
                if (latest != title) {
                    ocrText = title
                    currentEvent.postValue(searchEventTitle(title))
                }
                return
            }
        }
        ocrText = null
        currentEvent.postValue(null)
    }

    private enum class EventType {
        Main, Chara, Support
    }

    private fun detectEventType(img: Bitmap): EventType? {
        if (charaEventDetector.detect(img)) return EventType.Chara
        if (supportEventDetector.detect(img)) return EventType.Support
        if (mainEventTypeDetector.detect(img)) return EventType.Main
        return null
    }

    private fun getEventTitle(img: Bitmap): String {
        val target = eventTitleCropper.preProcess(img)
        return extractText(target)
    }

    private fun extractText(img: Bitmap): String {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        return text.replace(Regex("[\\s　]+"), "")
    }

    private suspend fun searchEventTitle(title: String): GameEvent? {
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
        } ?: throw NoSuchElementException("event list is empty")
    }

    private suspend fun calcTitleDistance(start: Int, end: Int, query: String, dst: Array<Float>) {
        if (start + 32 < end) {
            val mid = start + (end - start) / 2
            viewModelScope.apply {
                val left = async(Dispatchers.IO) {
                    calcTitleDistance(start, mid, query, dst)
                }
                val right = async(Dispatchers.IO) {
                    calcTitleDistance(mid, end, query, dst)
                }
                left.await()
                right.await()
            }
        } else {
            val algo = LevensteinDistance()
            (start until end).forEach { idx ->
                val event = events[idx]
                dst[idx] = algo.getDistance(event.eventTitle, query)
            }
        }
    }
}
