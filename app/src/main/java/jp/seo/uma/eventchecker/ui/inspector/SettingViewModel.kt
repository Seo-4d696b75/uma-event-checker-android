package jp.seo.uma.eventchecker.ui.inspector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import java.lang.Float.min
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
) : ViewModel() {

    val minInterval: StateFlow<Long> = settingRepository.minUpdateInterval
        .stateIn(viewModelScope, SharingStarted.Lazily)

    val ocrThreshold: StateFlow<Float> = settingRepository.ocrThreshold
        .stateIn(viewModelScope, SharingStarted.Lazily)

    fun setMinInterval(value: Long) =
        settingRepository.minUpdateInterval.update(viewModelScope) { max(100L, value) }


    fun setOcrThreshold(value: Float) =
        settingRepository.ocrThreshold.update(viewModelScope) { min(max(0.1f, value), 1.0f) }
}