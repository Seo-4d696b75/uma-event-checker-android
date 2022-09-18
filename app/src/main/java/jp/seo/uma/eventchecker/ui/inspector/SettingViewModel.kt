package jp.seo.uma.eventchecker.ui.inspector

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.Float.min
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
) : ViewModel() {

    val minInterval: StateFlow<Long> = settingRepository.minUpdateInterval
    val ocrThreshold: StateFlow<Float> = settingRepository.ocrThreshold

    fun setMinInterval(value: Long) =
        settingRepository.minUpdateInterval.update { max(100L, value) }

    fun setOcrThreshold(value: Float) =
        settingRepository.ocrThreshold.update { min(max(0.1f, value), 1.0f) }
}