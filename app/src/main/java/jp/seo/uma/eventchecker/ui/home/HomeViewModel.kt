package jp.seo.uma.eventchecker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.repository.AppRepository
import jp.seo.uma.eventchecker.repository.DataRepository
import jp.seo.uma.eventchecker.repository.ScreenCapture
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
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    dataRepository: DataRepository,
    imgProcess: ImageProcess,
    private val capture: ScreenCapture,
) : ViewModel() {

    val loading = combine(
        imgProcess.hasInitialized,
        dataRepository.initialized,
    ) { v1, v2 -> !v1 || !v2 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val runningCapture = capture.running

    fun toggleStartButton() = viewModelScope.launch {
        appRepository.startChecker(!capture.running.value)
    }

}
