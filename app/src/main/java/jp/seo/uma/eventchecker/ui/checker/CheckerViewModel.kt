package jp.seo.uma.eventchecker.ui.checker

import android.media.Image
import android.media.projection.MediaProjection
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import jp.seo.uma.eventchecker.data.repository.ScreenRepository
import jp.seo.uma.eventchecker.data.repository.SearchRepository
import jp.seo.uma.eventchecker.data.repository.SettingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class CheckerViewModel constructor(
    private val search: SearchRepository,
    private val setting: SettingRepository,
    private val capture: ScreenRepository,
) : ViewModel() {

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
        search.update(img)
        val wait = start + setting.minUpdateInterval - SystemClock.uptimeMillis()
        if (wait > 0L) {
            Log.d("update", "wait $wait ms")
            delay(wait)
        }
    }

    fun setScreenCallback(callback: ((Image) -> Unit)) = capture.setCallback(callback)

    fun startCapture(projection: MediaProjection) = capture.start(projection)

    fun stopCapture() = capture.stop()

    val currentEvent = search.currentEvent
}