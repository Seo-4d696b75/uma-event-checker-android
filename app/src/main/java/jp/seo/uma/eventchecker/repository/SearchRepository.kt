package jp.seo.uma.eventchecker.repository

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import jp.seo.uma.eventchecker.img.EventType
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.img.toMat
import jp.seo.uma.eventchecker.model.EventOwner
import jp.seo.uma.eventchecker.model.GameEvent
import jp.seo.uma.eventchecker.model.match
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.opencv.core.Mat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val dataRepository: DataRepository,
    private val settingRepository: SettingRepository,
    private val imageProcess: ImageProcess,
) {

    private var eventTitle: String? = null
    private var eventType: EventType? = null

    private val _isGameScreen = MutableStateFlow<Boolean>(false)

    /**
     * ゲーム画面の検出結果
     */
    val isGameScreen: StateFlow<Boolean> = _isGameScreen

    private val _eventType = MutableStateFlow<EventType?>(null)

    /**
     * 検出されたイベントタイプ
     */
    val currentEventType: StateFlow<EventType?> = _eventType

    private val _textImage = MutableStateFlow<Bitmap?>(null)

    /**
     * イベントタイトルを検出した元画像（白黒変換済み）
     */
    val textImage: StateFlow<Bitmap?> = _textImage

    private val _title = MutableStateFlow<String?>(null)

    /**
     * 検出したイベントタイトル文字列
     */
    val title: StateFlow<String?> = _title

    private val _currentEvent = MutableStateFlow<GameEvent?>(null)

    /**
     * 検出されたゲームイベント
     */
    val currentEvent: StateFlow<GameEvent?> = _currentEvent

    private fun getEventType(mat: Mat): EventType? {
        // check game screen
        val isGame = imageProcess.isGameScreen(mat)
        Log.d("Img", "check is-target $isGame")
        _isGameScreen.update { isGame }
        if (isGame) {
            // detect event type
            val type = imageProcess.getEventType(mat)
            Log.d("Img", "event type '${type.toString()}'")
            if (type != null) return type
        }

        eventType = null
        eventTitle = null
        _eventType.update { null }
        _title.update { null }
        _textImage.update { null }
        _currentEvent.update { null }
        return null
    }

    suspend fun searchForEvent(img: Image) {
        // copy image
        val mat = imageProcess.copyToBitmap(img).toMat()

        // detect event type
        val type = getEventType(mat) ?: return
        _eventType.update { type }

        // run ocr
        val ocrResult = imageProcess.getEventTitle(mat, type)
        val gray = ocrResult.first
        val title = ocrResult.second
        _textImage.update { gray }
        _title.update { title }

        // skip if no change
        if (eventType == type && eventTitle == title) {
            return
        }
        eventType = type
        eventTitle = title

        // search for events
        val th = settingRepository.ocrThread.value
        val events = dataRepository.searchForEvent(title, th, type).map { it.event }

        if (events.isEmpty()) {
            // event not found
            _currentEvent.update { null }
        } else if (events.size == 1) {
            // exactly single event found
            _currentEvent.update { events[0] }
        } else {
            // if more than one event found,
            // detect event owner and filter them
            val owner = if (type == EventType.Scenario) {
                EventOwner.Scenario("unknown") // シナリオの種類は判別しない
            } else {
                imageProcess.getEventOwner(mat, type).uma.toEventOwner()
            }
            assert(owner.match(type))
            var event = events[0]
            val filter = events.filter { it.owner.match(owner) }
            if (filter.isNotEmpty()) {
                // select event with max score from filtered list
                event = filter[0]
                Log.d("EventData", "filtered size ${filter.size}, [0]-> ${event.title}")
            } else {
                // select event with max score from not-filtered list
                Log.d("EventData", "no event remains after filter")
            }
            _currentEvent.update { event }
        }
    }
}

fun EventOwner.match(other: EventOwner) = when (this) {
    is EventOwner.Scenario -> other is EventOwner.Scenario // シナリオの種類を区別しない
    else -> this == other // 完全一致
}