package jp.seo.uma.eventchecker.ui

import android.media.projection.MediaProjection
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.repository.AppRepository
import jp.seo.uma.eventchecker.repository.DataRepository
import jp.seo.uma.eventchecker.repository.ScreenCapture
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val dataRepository: DataRepository,
    private val imgProcess: ImageProcess,
    private val settingRepository: SettingRepository,
    private val capture: ScreenCapture,
) : ViewModel() {

    val event = appRepository.event

    /**
     * 新しいデータを確認する
     */
    fun checkDataUpdate() = viewModelScope.launch {
        try {
            val info = dataRepository.checkUpdate()
            if (info == null) {
                loadData()
            } else {
                appRepository.requestUpdateData(info)
            }
        } catch (e: Exception) {
            appRepository.emitError(e)
        }
    }

    /**
     * ストレージからデータ読み出す
     */
    fun loadData() = viewModelScope.launch {
        try {
            dataRepository.loadData()
            imgProcess.init()
        } catch (e: Exception) {
            appRepository.emitError(e)
        }
    }

    fun startCapture(projection: MediaProjection) = capture.start(projection)

    fun stopCapture() = capture.stop()

    fun setMetrics(manager: WindowManager) = settingRepository.setMetrics(manager)

}