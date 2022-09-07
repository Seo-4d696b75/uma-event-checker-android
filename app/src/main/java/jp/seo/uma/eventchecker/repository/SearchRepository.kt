package jp.seo.uma.eventchecker.repository

import android.util.Log
import jp.seo.uma.eventchecker.model.GameEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val dataRepository: DataRepository,
) {

    private var eventTitle: String? = null

    private val _currentEvent = MutableStateFlow<GameEvent?>(null)

    val currentEvent: StateFlow<GameEvent?> = _currentEvent

    fun searchForEvent(title: String?): List<GameEvent>? {
        if (eventTitle != title) {
            eventTitle = title
            return if (title == null) {
                emptyList()
            } else {
                dataRepository.searchForEvent(title)
            }
        }
        return null
    }


    fun setCurrentEvent(events: List<GameEvent>, ownerName: String?) {
        if (events.isEmpty()) {
            _currentEvent.update { null }
        } else if (events.size == 1 || ownerName == null) {
            _currentEvent.update { events[0] }
        } else {
            var event = events[0]
            val filter = events.filter { it.ownerName == ownerName }
            if (filter.isNotEmpty()) {
                event = filter[0]
                Log.d("EventData", "filtered size ${filter.size}, [0]-> ${event.title}")
            } else {
                Log.d("EventData", "no event remains after filter")
            }
            _currentEvent.update { event }
        }
    }
}