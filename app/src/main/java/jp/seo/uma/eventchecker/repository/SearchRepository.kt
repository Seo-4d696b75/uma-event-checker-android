package jp.seo.uma.eventchecker.repository

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

    private val _currentEvent = MutableStateFlow<GameEvent?>(null)

    val currentEvent: StateFlow<GameEvent?> = _currentEvent

    suspend fun searchForEvent(img: Image) {
        // copy image
        val mat = imageProcess.copyToBitmap(img).toMat()
        // run image processing and ocr
        val ocrResult = imageProcess.getEventTitle(mat)
        if (ocrResult == null) {
            _currentEvent.update { null }
            return
        }
        // event type and title
        val type = ocrResult.first
        val title = ocrResult.second
        if (eventType == type && eventTitle == title) {
            // skip if no change
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
            val owner = if(type == EventType.Scenario){
                EventOwner.Scenario("unknown") // シナリオの種類は判別しない
            } else {
                imageProcess.getEventOwner(mat).uma.toEventOwner()
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

fun EventOwner.match(other: EventOwner) = when(this) {
    is EventOwner.Scenario -> other is EventOwner.Scenario // シナリオの種類を区別しない
    else -> this == other // 完全一致
}