package jp.seo.uma.eventchecker.ui

import android.media.Image
import android.media.projection.MediaProjection
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.img.toMat
import jp.seo.uma.eventchecker.repository.AppRepository
import jp.seo.uma.eventchecker.repository.DataRepository
import jp.seo.uma.eventchecker.repository.ScreenCapture
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val dataRepository: DataRepository,
    private val imgProcess: ImageProcess,
    private val settingRepository: SettingRepository,
    private val capture: ScreenCapture,
) : ViewModel() {

    val loading: LiveData<Boolean> = imgProcess.hasInitialized.map { !it }

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
     * Gets data from disk and init
     */
    fun loadData() = viewModelScope.launch {
        try {
            dataRepository.loadData()
            imgProcess.init()
        } catch (e: Exception) {
            appRepository.emitError(e)
        }
    }

    fun setMetrics(manager: WindowManager) = settingRepository.setMetrics(manager)

    /**
     * Note: this func is supposed to be called from a callback set at ImageReader,
     * which is running NOT on main thread
     */
    fun updateScreen(img: Image) = runBlocking {
        /*
        ImageReader provides image data via a callback, whereas
        this ViewModel uses coroutine.
        runBlocking is used in order to call suspending style functions in blocking style
         */
        val start = SystemClock.uptimeMillis()
        val mat = imgProcess.copyToBitmap(img).toMat()
        val title = imgProcess.getEventTitle(mat)
        dataRepository.searchForEvent(title)?.let { events ->
            val ownerName = if (events.size <= 1) null else {
                imgProcess.getEventOwner(mat)
            }
            dataRepository.setCurrentEvent(events, ownerName)
        }
        val now = SystemClock.uptimeMillis()
        val wait = start + settingRepository.minUpdateInterval - now
        if (wait > 0L) {
            Log.d(
                "ViewModel",
                "update -> wait $wait ms (min-interval ${settingRepository.minUpdateInterval} ms)"
            )
            delay(wait)
        } else {
            Log.d("ViewModel", "update -> time ${now - start} ms")
        }
    }

    fun startCapture(projection: MediaProjection) = capture.start(projection)

    fun stopCapture() = capture.stop()

    val runningCapture = capture.running

    fun setScreenCallback(callback: ((Image) -> Unit)) {
        capture.callback = callback
    }

}