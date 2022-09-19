package jp.seo.uma.eventchecker.ui.checker

import android.media.Image
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.seo.uma.eventchecker.repository.ScreenCapture
import jp.seo.uma.eventchecker.repository.SearchRepository
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.runBlocking

class CheckerViewModel constructor(
    private val capture: ScreenCapture,
    settingRepository: SettingRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {

    fun setScreenCallback(callback: ((Image) -> Unit)) {
        capture.callback = callback
    }

    fun stopCapture() = capture.stop()

    // observe setting values
    private val minUpdateInterval =
        settingRepository.minUpdateInterval.stateIn(viewModelScope, SharingStarted.Lazily)
    private val ocrThreshold =
        settingRepository.ocrThreshold.stateIn(viewModelScope, SharingStarted.Lazily)

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
        searchRepository.searchForEvent(img, ocrThreshold.value)
        val now = SystemClock.uptimeMillis()
        val minInterval = minUpdateInterval.value
        val wait = start + minInterval - now
        if (wait > 0L) {
            Log.d(
                "ViewModel",
                "update -> wait $wait ms (min-interval $minInterval ms)"
            )
            delay(wait)
        } else {
            Log.d("ViewModel", "update -> time ${now - start} ms")
        }
    }
}