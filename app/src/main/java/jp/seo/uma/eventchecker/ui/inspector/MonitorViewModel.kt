package jp.seo.uma.eventchecker.ui.inspector

import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.EventType
import jp.seo.uma.eventchecker.model.GameEvent
import jp.seo.uma.eventchecker.repository.SearchRepository
import jp.seo.uma.eventchecker.repository.SettingRepository
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val setting: SettingRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {

    @MainThread
    fun setUiState() {
        _isGameScreen = searchRepository.isGameScreen.value
        _ocrText = searchRepository.title.value
        _textImg = searchRepository.textImage.value
        _eventType = searchRepository.currentEventType.value
        _currentEvent = searchRepository.currentEvent.value
    }

    private var _isGameScreen: Boolean = false
    private var _ocrText: String? = null
    private var _textImg: Bitmap? = null
    private var _eventType: EventType? = null
    private var _currentEvent: GameEvent? = null

    val isGameScreen
        get() = _isGameScreen

    val ocrText
        get() = _ocrText

    val textImg
        get() = _textImg

    val eventType
        get() = _eventType

    val currentEvent
        get() = _currentEvent

    fun onDismiss() {
        setting.isDebugDialogShown.value = false
    }
}