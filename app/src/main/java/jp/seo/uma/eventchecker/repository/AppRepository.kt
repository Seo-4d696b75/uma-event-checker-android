package jp.seo.uma.eventchecker.repository

import jp.seo.uma.eventchecker.api.EventDataInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor() {
    private val _event = MutableSharedFlow<AppEvent>()

    /**
     * アプリ全体でのイベントを通知する
     */
    val event: SharedFlow<AppEvent> = _event

    suspend fun requestUpdateData(info: EventDataInfo) {
        _event.emit(AppEvent.DataUpdateRequest(info))
    }

    suspend fun confirmUpdateData(info: EventDataInfo, confirm: Boolean) {
        _event.emit(AppEvent.DataUpdateConfirm(info, confirm))
    }

    suspend fun emitError(exception: Exception) {
        _event.emit(AppEvent.Error(exception))
    }

    suspend fun startChecker(start: Boolean) {
        _event.emit(
            if (start) AppEvent.StartChecker else AppEvent.StopChecker
        )
    }
}

sealed interface AppEvent {
    data class DataUpdateRequest(val info: EventDataInfo) : AppEvent
    data class DataUpdateConfirm(val info: EventDataInfo, val confirm: Boolean) : AppEvent
    data class Error(val e: Exception) : AppEvent
    object StartChecker : AppEvent
    object StopChecker : AppEvent
}
