package jp.seo.uma.eventchecker.ui

import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.core.DataRepository
import jp.seo.uma.eventchecker.core.SettingRepository
import jp.seo.uma.eventchecker.img.EventType
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.model.GameEvent
import javax.inject.Inject

@HiltViewModel
class DebugDialogViewModel @Inject constructor(
    private val imgProcess: ImageProcess,
    private val setting: SettingRepository,
    private val dataRepository: DataRepository,
) : ViewModel() {

    @MainThread
    fun setUiState() {
        _isGameScreen = imgProcess.isGameScreen.value ?: false
        _ocrText = imgProcess.title.value
        _textImg = imgProcess.textImage.value
        _eventType = imgProcess.currentEventType.value
        _currentEvent = dataRepository.currentEvent.value
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