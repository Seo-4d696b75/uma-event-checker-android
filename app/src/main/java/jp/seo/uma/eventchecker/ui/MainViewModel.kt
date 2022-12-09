package jp.seo.uma.eventchecker.ui

import android.content.Context
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.data.repository.DataRepository
import jp.seo.uma.eventchecker.data.repository.ScreenRepository
import jp.seo.uma.eventchecker.data.repository.SettingRepository
import jp.seo.uma.eventchecker.img.ImageProcess
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val imgProcess: ImageProcess,
    private val setting: SettingRepository,
    capture: ScreenRepository,
) : ViewModel() {

    val loading = combine(
        dataRepository.initialized,
        imgProcess.initialized,
    ) { v1, v2 -> !v1 || !v2 }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            true,
        )

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        imgProcess.init(context)
        dataRepository.init(context)
    }

    fun setMetrics(manager: WindowManager) = setting.setMetrics(manager)

    val runningCapture = capture.running
}
